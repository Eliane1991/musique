/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphael Slinckx <raphael@slinckx.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.audio.generic;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.ModifyVetoException;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This abstract class is the skeleton for tag writers.
 * <p/>
 * <p>It handles the creation/closing of the randomaccessfile objects and then call the subclass
 * method writeTag or deleteTag. These two method have to be implemented in the
 * subclass.
 *
 * @author Raphael Slinckx
 * @version $Id: AudioFileWriter.java,v 1.18 2008/11/28 22:14:44 paultaylor Exp $
 * @since v0.02
 */
public abstract class AudioFileWriter {
    private static final String TEMP_FILENAME_SUFFIX = ".tmp";
    private static final String WRITE_MODE = "rw";
    private static final int MINIMUM_FILESIZE = 150;

    // Logger Object
    public static Logger logger = LoggerFactory.getLogger("org.jaudiotagger.audio.generic");

    /**
     * If not <code>null</code>, this listener is used to notify the listener
     * about modification events.<br>
     */
    private AudioFileModificationListener modificationListener = null;

    /**
     * Delete the tag (if any) present in the given file
     *
     * @param af The file to process
     * @throws CannotWriteException if anything went wrong
     */
    public synchronized void delete(AudioFile af) throws CannotReadException, CannotWriteException {
        if (!af.getFile().canWrite()) {
            throw new CannotWriteException(ErrorMessage.GENERAL_DELETE_FAILED.getMsg(af.getFile().getPath()));
        }

        if (af.getFile().length() <= MINIMUM_FILESIZE) {
            throw new CannotWriteException(ErrorMessage.GENERAL_DELETE_FAILED.getMsg(af.getFile().getPath()));
        }

        RandomAccessFile raf = null;
        RandomAccessFile rafTemp = null;
        File tempF = null;

        //Will be set to true on VetoException, causing the finally block to discard the tempfile.
        boolean revert = false;

        try {

            tempF = File.createTempFile(af.getFile().getName().replace('.', '_'), TEMP_FILENAME_SUFFIX, af.getFile().getParentFile());
            rafTemp = new RandomAccessFile(tempF, WRITE_MODE);
            raf = new RandomAccessFile(af.getFile(), WRITE_MODE);
            raf.seek(0);
            rafTemp.seek(0);

            try {
                if (this.modificationListener != null) {
                    this.modificationListener.fileWillBeModified(af, true);
                }
                deleteTag(raf, rafTemp);
                if (this.modificationListener != null) {
                    this.modificationListener.fileModified(af, tempF);
                }
            } catch (ModifyVetoException veto) {
                throw new CannotWriteException(veto);
            }

        } catch (Exception e) {
            revert = true;
            throw new CannotWriteException("\"" + af.getFile().getAbsolutePath() + "\" :" + e, e);
        } finally {
            // will be set to the remaining file.
            File result = af.getFile();
            try {
                if (raf != null) {
                    raf.close();
                }
                if (rafTemp != null) {
                    rafTemp.close();
                }

                if (tempF.length() > 0 && !revert) {
                    boolean deleteResult = af.getFile().delete();
                    if (deleteResult == false) {
                            //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_ORIGINAL_FILE.getMsg(af.getFile().getPath(), tempF.getPath()));
                        throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_ORIGINAL_FILE.getMsg(af.getFile().getPath(), tempF.getPath()));
                    }
                    boolean renameResult = tempF.renameTo(af.getFile());
                    if (renameResult == false) {
                            //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(af.getFile().getPath(), tempF.getPath()));
                        throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(af.getFile().getPath(), tempF.getPath()));
                    }
                    result = tempF;

                    //If still exists we can now delete
                    if (tempF.exists()) {
                        if (!tempF.delete()) {
                            //Non critical failed deletion
                            //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(tempF.getPath()));
                        }
                    }
                } else {
                    //It was created but never used
                    if (!tempF.delete()) {
                        //Non critical failed deletion
                        //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(tempF.getPath()));
                    }
                }
            } catch (Exception ex) {
//                //logger.error("AudioFileWriter exception cleaning up delete:" + af.getFile().getPath() + " or" + tempF.getAbsolutePath() + ":" + ex);
            }
            // Notify listener
            if (this.modificationListener != null) {
                this.modificationListener.fileOperationFinished(result);
            }
        }
    }

    /**
     * Delete the tag (if any) present in the given randomaccessfile, and do not
     * close it at the end.
     *
     * @param raf     The source file, already opened in r-write mode
     * @param tempRaf The temporary file opened in r-write mode
     * @throws CannotWriteException if anything went wrong
     */
    public synchronized void delete(RandomAccessFile raf, RandomAccessFile tempRaf) throws CannotReadException, CannotWriteException, IOException {
        raf.seek(0);
        tempRaf.seek(0);
        deleteTag(raf, tempRaf);
    }

    /**
     * Same as above, but delete tag in the file.
     *
     * @throws IOException          is thrown when the RandomAccessFile operations throw it
     *                              (you should never throw them manually)
     * @throws CannotWriteException when an error occured during the deletion of the tag
     */
    protected abstract void deleteTag(RandomAccessFile raf, RandomAccessFile tempRaf) throws CannotReadException, CannotWriteException, IOException;

    /**
     * This method sets the {@link AudioFileModificationListener}.<br>
     * There is only one listener allowed, if you want more instances to be
     * supported, use the {@link ModificationHandler} to broadcast those events.<br>
     *
     * @param listener The listener. <code>null</code> allowed to deregister.
     */
    public synchronized void setAudioFileModificationListener(AudioFileModificationListener listener) {
        this.modificationListener = listener;
    }

    /**
     * Prechecks before normal write
     * <p/>
     * <ul>
     * <li>If the tag is actually empty, remove the tag</li>
     * <li>if the file is not writable, throw exception<li>
     * <li>If the file is too small to be a valid file, throw exception<li>
     * </ul>
     *
     * @param af
     * @throws CannotWriteException
     */
    private void precheckWrite(AudioFile af) throws CannotWriteException {
        // Preliminary checks
        try {
            if (af.getTag().isEmpty()) {
                delete(af);
                return;
            }
        } catch (CannotReadException re) {
            throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED.getMsg(af.getFile().getPath()));
        }

        if (!af.getFile().canWrite()) {
//            //logger.error(ErrorMessage.GENERAL_WRITE_FAILED.getMsg(af.getFile().getPath()));
            throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED.getMsg(af.getFile().getPath()));
        }

        if (af.getFile().length() <= MINIMUM_FILESIZE) {
//            //logger.error(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_IS_TOO_SMALL.getMsg(af.getFile().getPath()));
            throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_IS_TOO_SMALL.getMsg(af.getFile().getPath()));
        }
    }

    /**
     * Write the tag (if not empty) present in the AudioFile in the associated
     * File
     *
     * @param af The file we want to process
     * @throws CannotWriteException if anything went wrong
     */
    //TODO Creates temp file in same folder as the original file, this is safe but would impose a performance
    //overhead if the original file is on a networked drive
    public synchronized void write(AudioFile af) throws CannotWriteException {
//        //logger.info("Started writing tag data for file:" + af.getFile().getName());

        //Prechecks
        precheckWrite(af);

        RandomAccessFile raf = null;
        RandomAccessFile rafTemp = null;
        File newFile = null;
        File result = null;

        //Create temporary File
        try {
            newFile = File.createTempFile(af.getFile().getName().replace('.', '_'), TEMP_FILENAME_SUFFIX, af.getFile().getParentFile());
        }
        //Unable to create temporary file, can happen in Vista if have Create Files/Write Data set to Deny
        catch (IOException ioe) {
//            //logger.error(ErrorMessage.GENERAL_WRITE_FAILED_TO_CREATE_TEMPORARY_FILE_IN_FOLDER.getMsg(af.getFile().getName(), af.getFile().getParentFile().getAbsolutePath()), ioe);
            throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED_TO_CREATE_TEMPORARY_FILE_IN_FOLDER.getMsg(af.getFile().getName(),
                    af.getFile().getParentFile().getAbsolutePath()));
        }

        //Open temporary file and actual file for Editing
        try {
            rafTemp = new RandomAccessFile(newFile, WRITE_MODE);
            raf = new RandomAccessFile(af.getFile(), WRITE_MODE);

        }
        //Unable to write to writable file, can happen in Vista if have Create Folders/Append Data set to Deny
        catch (IOException ioe) {
//            //logger.error(ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING.getMsg(af.getFile().getAbsolutePath()), ioe);

            //If we managed to open either file, delete it.
            try {
                if (raf != null) {
                    raf.close();
                }
                if (rafTemp != null) {
                    rafTemp.close();
                }
            } catch (IOException ioe2) {
                //Warn but assume has worked okay
//                //logger.warn( ErrorMessage.GENERAL_WRITE_PROBLEM_CLOSING_FILE_HANDLE.getMsg(af.getFile(), ioe.getMessage()), ioe2);
            }

            //Delete the temp file ( we cannot delet until closed correpsonding rafTemp)
            if (!newFile.delete()) {
                //Non critical failed deletion
                //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(newFile.getAbsolutePath()));
            }

            throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING.getMsg(af.getFile().getAbsolutePath()));
        }

        //Write data to File
        try {

            raf.seek(0);
            rafTemp.seek(0);
            try {
                if (this.modificationListener != null) {
                    this.modificationListener.fileWillBeModified(af, false);
                }
                writeTag(af.getTag(), raf, rafTemp);
                if (this.modificationListener != null) {
                    this.modificationListener.fileModified(af, newFile);
                }
            } catch (ModifyVetoException veto) {
                throw new CannotWriteException(veto);
            }
        } catch (Exception e) {
//            //logger.error(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE.getMsg(af.getFile(), e.getMessage()), e);

            try {
                if (raf != null) {
                    raf.close();
                }
                if (rafTemp != null) {
                    rafTemp.close();
                }
            } catch (IOException ioe) {
                //Warn but assume has worked okay
//                //logger.warn( ErrorMessage.GENERAL_WRITE_PROBLEM_CLOSING_FILE_HANDLE.getMsg(af.getFile().getAbsolutePath(), ioe.getMessage()), ioe);
            }

            //Delete the temporary file because either it was never used so lets just tidy up or we did start writing to it but
            //the write failed and we havent renamed it back to the original file so we can just delete it.
            if (!newFile.delete()) {
                //Non critical failed deletion
                //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(newFile.getAbsolutePath()));
            }
            throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE.getMsg(af.getFile(), e.getMessage()));
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
                if (rafTemp != null) {
                    rafTemp.close();
                }
            } catch (IOException ioe) {
                //Warn but assume has worked okay
//                //logger.warn( ErrorMessage.GENERAL_WRITE_PROBLEM_CLOSING_FILE_HANDLE.getMsg(af.getFile().getAbsolutePath(), ioe.getMessage()), ioe);
            }
        }

        //Result held in this file
        result = af.getFile();

        //If the temporary file was used
        if (newFile.length() > 0) {
            //Rename Original File
            //Can fail on Vista if have Special Permission 'Delete' set Deny
            File originalFileBackup = new File(af.getFile().getParentFile().getPath(), AudioFile.getBaseFilename(af.getFile()) + ".old");
            boolean renameResult = af.getFile().renameTo(originalFileBackup);
            if (renameResult == false) {
//                //logger.warn(Level.SEVERE,ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_ORIGINAL_FILE_TO_BACKUP.getMsg(af.getFile().getPath(), originalFileBackup.getName()));
                throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_ORIGINAL_FILE_TO_BACKUP.getMsg(af.getFile().getPath(), originalFileBackup.getName()));
            }

            //Rename Temp File to Original File
            renameResult = newFile.renameTo(af.getFile());
            if (!renameResult) {
                //Renamed failed so lets do some checks rename the backup back to the original file
                //New File doesnt exist
                if (!newFile.exists()) {
                    //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_NEW_FILE_DOESNT_EXIST.getMsg(newFile.getAbsolutePath()));
                }

                //Rename the backup back to the original
                if (!originalFileBackup.renameTo(af.getFile())) {
                    //TODO now if this happens we are left with testfile.old instead of testfile.mp4
                    //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_ORIGINAL_BACKUP_TO_ORIGINAL.getMsg(originalFileBackup.getAbsolutePath(), af.getFile().getName()));
                }

                //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(af.getFile().getAbsolutePath(), newFile.getName()));
                throw new CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(af.getFile().getAbsolutePath(), newFile.getName()));
            } else {
                //Rename was okay so we can now delete the backup of the original
                boolean deleteResult = originalFileBackup.delete();
                if (!deleteResult) {
                    //Not a disaster but can't delete the backup so make a warning
                    //logger.warn(ErrorMessage.GENERAL_WRITE_WARNING_UNABLE_TO_DELETE_BACKUP_FILE.getMsg(originalFileBackup.getAbsolutePath()));
                }
            }

            //Delete the temporary file if still exists
            if (newFile.exists()) {
                if (!newFile.delete()) {
                    //Non critical failed deletion
                    //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(newFile.getPath()));
                }
            }
        } else {
            //Delete the temporary file that wasn't ever used           
            if (!newFile.delete()) {
                //Non critical failed deletion
                //logger.warn(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(newFile.getPath()));
            }
        }

        if (this.modificationListener != null) {
            this.modificationListener.fileOperationFinished(result);
        }
    }

    /**
     * This is called when a tag has to be written in a file. Three parameters
     * are provided, the tag to write (not empty) Two randomaccessfiles, the
     * first points to the file where we want to write the given tag, and the
     * second is an empty temporary file that can be used if e.g. the file has
     * to be bigger than the original.
     * <p/>
     * If something has been written in the temporary file, when this method
     * returns, the original file is deleted, and the temporary file is renamed
     * the the original name
     * <p/>
     * If nothing has been written to it, it is simply deleted.
     * <p/>
     * This method can assume the raf, rafTemp are pointing to the first byte of
     * the file. The subclass must not close these two files when the method
     * returns.
     *
     * @throws IOException          is thrown when the RandomAccessFile operations throw it
     *                              (you should never throw them manually)
     * @throws CannotWriteException when an error occured during the generation of the tag
     */
    protected abstract void writeTag(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp) throws CannotReadException, CannotWriteException, IOException;

}

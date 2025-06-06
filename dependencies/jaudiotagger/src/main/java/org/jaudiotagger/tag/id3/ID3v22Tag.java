/*
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can getFields a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jaudiotagger.tag.id3;

import org.jaudiotagger.FileConstants;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.datatype.Artwork;
import org.jaudiotagger.tag.datatype.DataTypes;
import org.jaudiotagger.tag.id3.framebody.*;
import org.jaudiotagger.tag.id3.valuepair.ImageFormats;
import org.jaudiotagger.tag.reference.PictureTypes;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Represents an ID3v2.2 tag.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id: ID3v22Tag.java 919 2010-10-04 13:46:39Z paultaylor $
 */
public class ID3v22Tag extends AbstractID3v2Tag {

    protected static final String TYPE_COMPRESSION = "compression";
    protected static final String TYPE_UNSYNCHRONISATION = "unsyncronisation";

    /**
     * Bit mask to indicate tag is Unsychronization
     */
    public static final int MASK_V22_UNSYNCHRONIZATION = FileConstants.BIT7;

    /**
     * Bit mask to indicate tag is compressed, although compression is not
     * actually defined in v22 so just ignored
     */
    public static final int MASK_V22_COMPRESSION = FileConstants.BIT6;

    /**
     * The tag is compressed, although no compression scheme is defined in ID3v22
     */
    protected boolean compression = false;

    /**
     * If set all frames in the tag uses unsynchronisation
     */
    protected boolean unsynchronization = false;

    public static final byte RELEASE = 2;
    public static final byte MAJOR_VERSION = 2;
    public static final byte REVISION = 0;

    /**
     * Retrieve the Release
     */
    public byte getRelease() {
        return RELEASE;
    }

    /**
     * Retrieve the Major Version
     */
    public byte getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
     * Retrieve the Revision
     */
    public byte getRevision() {
        return REVISION;
    }

    /**
     * Creates a new empty ID3v2_2 tag.
     */
    public ID3v22Tag() {
        frameMap = new LinkedHashMap();
        encryptedFrameMap = new LinkedHashMap();
    }

    /**
     * Copy primitives applicable to v2.2
     */
    protected void copyPrimitives(AbstractID3v2Tag copyObj) {
        //logger.info("Copying primitives");
        super.copyPrimitives(copyObj);

        //Set the primitive types specific to v2_2.
        if (copyObj instanceof ID3v22Tag) {
            ID3v22Tag copyObject = (ID3v22Tag) copyObj;
            this.compression = copyObject.compression;
            this.unsynchronization = copyObject.unsynchronization;
        } else if (copyObj instanceof ID3v23Tag) {
            ID3v23Tag copyObject = (ID3v23Tag) copyObj;
            this.compression = copyObject.compression;
            this.unsynchronization = copyObject.unsynchronization;
        } else if (copyObj instanceof ID3v24Tag) {
            ID3v24Tag copyObject = (ID3v24Tag) copyObj;
            this.compression = false;
            this.unsynchronization = copyObject.unsynchronization;
        }
    }


    /**
     * Copy Constructor, creates a new ID3v2_2 Tag based on another ID3v2_2 Tag
     *
     * @param copyObject
     */
    public ID3v22Tag(ID3v22Tag copyObject) {
        //This doesnt do anything.
        super(copyObject);
        //logger.info("Creating tag from another tag of same type");
        copyPrimitives(copyObject);
        copyFrames(copyObject);
    }

    /**
     * Constructs a new tag based upon another tag of different version/type
     *
     * @param mp3tag
     */
    public ID3v22Tag(AbstractTag mp3tag) {
        frameMap = new LinkedHashMap();
        encryptedFrameMap = new LinkedHashMap();
        //logger.info("Creating tag from a tag of a different version");
        //Default Superclass constructor does nothing
        if (mp3tag != null) {
            ID3v24Tag convertedTag;
            //Should use the copy constructor instead
            if ((!(mp3tag instanceof ID3v23Tag)) && (mp3tag instanceof ID3v22Tag)) {
                throw new UnsupportedOperationException("Copy Constructor not called. Please type cast the argument");
            }
            //If v2.4 can getFields variables from this
            else if (mp3tag instanceof ID3v24Tag) {
                convertedTag = (ID3v24Tag) mp3tag;
            }
            //Any tag (e.g lyrics3 and idv1.1,idv2.3 can be converted to id32.4 so do that
            //to simplify things
            else {
                convertedTag = new ID3v24Tag(mp3tag);
            }
            this.setLoggingFilename(convertedTag.getLoggingFilename());
            //Set the primitive types specific to v2_2.
            copyPrimitives(convertedTag);
            //Set v2.2 Frames
            copyFrames(convertedTag);
            //logger.info("Created tag from a tag of a different version");
        }
    }

    /**
     * Creates a new ID3v2_2 datatype.
     *
     * @param buffer
     * @param loggingFilename
     * @throws TagException
     */
    public ID3v22Tag(ByteBuffer buffer, String loggingFilename) throws TagException {
        setLoggingFilename(loggingFilename);
        this.read(buffer);
    }


    /**
     * Creates a new ID3v2_2 datatype.
     *
     * @param buffer
     * @throws TagException
     * @deprecated use {@link #ID3v22Tag(ByteBuffer, String)} instead
     */
    public ID3v22Tag(ByteBuffer buffer) throws TagException {
        this(buffer, "");
    }

    /**
     * @return an indentifier of the tag type
     */
    public String getIdentifier() {
        return "ID3v2_2.20";
    }

    /**
     * Return frame size based upon the sizes of the frames rather than the size
     * including padding recorded in the tag header
     *
     * @return size
     */
    public int getSize() {
        int size = TAG_HEADER_LENGTH;
        size += super.getSize();
        return size;
    }


    /**
     * @param obj
     * @return equality
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ID3v22Tag)) {
            return false;
        }
        ID3v22Tag object = (ID3v22Tag) obj;
        if (this.compression != object.compression) {
            return false;
        }
        return this.unsynchronization == object.unsynchronization && super.equals(obj);
    }


    protected void addFrame(AbstractID3v2Frame frame) {
        try {
            //Special case to handle TDRC frame from V24 that needs breaking up into separate frame in V23
            if ((frame.getIdentifier().equals(ID3v24Frames.FRAME_ID_YEAR)) && (frame.getBody() instanceof FrameBodyTDRC)) {
                translateFrame(frame);
            } else if (frame instanceof ID3v22Frame) {
                copyFrameIntoMap(frame.getIdentifier(), frame);
            } else {
                ID3v22Frame newFrame = new ID3v22Frame(frame);
                copyFrameIntoMap(newFrame.getIdentifier(), newFrame);
            }
        } catch (InvalidFrameException ife) {
            logger.error("Unable to convert frame:" + frame.getIdentifier());
        }
    }

    /**
     * Read the size of a tag, based on  the value written in the tag header
     *
     * @param buffer
     * @return
     * @throws TagException
     */
    public int readSize(ByteBuffer buffer) {
        //Skip over flags
        byte flags = buffer.get();

        // Read the size, this is size of tag not including  the tag header
        int size = ID3SyncSafeInteger.bufferToValue(buffer);

        //Return the exact size of tag as setField in the tag header
        return size + TAG_HEADER_LENGTH;
    }

    /**
     * Read tag Header Flags
     *
     * @param byteBuffer
     * @throws TagException
     */
    private void readHeaderFlags(ByteBuffer byteBuffer) throws TagException {
        //Flags
        byte flags = byteBuffer.get();
        unsynchronization = (flags & MASK_V22_UNSYNCHRONIZATION) != 0;
        compression = (flags & MASK_V22_COMPRESSION) != 0;

        if (unsynchronization) {
            //logger.info(ErrorMessage.ID3_TAG_UNSYNCHRONIZED.getMsg(getLoggingFilename()));
        }

        if (compression) {
            //logger.info(ErrorMessage.ID3_TAG_COMPRESSED.getMsg(getLoggingFilename()));
        }

        //Not allowable/Unknown Flags
        if ((flags & FileConstants.BIT5) != 0) {
            //logger.warn(ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(getLoggingFilename(), FileConstants.BIT5));
        }
        if ((flags & FileConstants.BIT4) != 0) {
            //logger.warn(ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(getLoggingFilename(), FileConstants.BIT4));
        }
        if ((flags & FileConstants.BIT3) != 0) {
            //logger.warn(ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(getLoggingFilename(), FileConstants.BIT3));
        }
        if ((flags & FileConstants.BIT2) != 0) {
            //logger.warn(ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(getLoggingFilename(), FileConstants.BIT2));
        }
        if ((flags & FileConstants.BIT1) != 0) {
            //logger.warn(ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(getLoggingFilename(), FileConstants.BIT1));
        }
        if ((flags & FileConstants.BIT0) != 0) {
            //logger.warn(ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(getLoggingFilename(), FileConstants.BIT3));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(ByteBuffer byteBuffer) throws TagException {
        int size;
        if (!seek(byteBuffer)) {
            throw new TagNotFoundException("ID3v2.20 tag not found");
        }
        //logger.info(getLoggingFilename() + ":" + "Reading tag from file");

        //Read the flags
        readHeaderFlags(byteBuffer);

        // Read the size
        size = ID3SyncSafeInteger.bufferToValue(byteBuffer);

        //Slice Buffer, so position markers tally with size (i.e do not include tagheader)
        ByteBuffer bufferWithoutHeader = byteBuffer.slice();

        //We need to synchronize the buffer
        if (unsynchronization) {
            bufferWithoutHeader = ID3Unsynchronization.synchronize(bufferWithoutHeader);
        }
        readFrames(bufferWithoutHeader, size);
        //logger.info(getLoggingFilename() + ":" + "Loaded Frames,there are:" + frameMap.keySet().size());
    }

    /**
     * Read frames from tag
     *
     * @param byteBuffer
     * @param size
     */
    protected void readFrames(ByteBuffer byteBuffer, int size) {
        //Now start looking for frames
        ID3v22Frame next;
        frameMap = new LinkedHashMap();
        encryptedFrameMap = new LinkedHashMap();

        //Read the size from the Tag Header
        this.fileReadSize = size;
        //logger.info(getLoggingFilename() + ":" + "Start of frame body at:" + byteBuffer.position() + ",frames sizes and padding is:" + size);
        /* todo not done yet. Read the first Frame, there seems to be quite a
         ** common case of extra data being between the tag header and the first
         ** frame so should we allow for this when reading first frame, but not subsequent frames
         */
        // Read the frames until got to upto the size as specified in header
        while (byteBuffer.position() < size) {
            try {
                //Read Frame
                //logger.info(getLoggingFilename() + ":" + "looking for next frame at:" + byteBuffer.position());
                next = new ID3v22Frame(byteBuffer, getLoggingFilename());
                String id = next.getIdentifier();
                loadFrameIntoMap(id, next);
            }
            //Found Padding, no more frames
            catch (PaddingException ex) {
                logger.info(getLoggingFilename() + ":Found padding starting at:" + byteBuffer.position());
                break;
            }
            //Found Empty Frame
            catch (EmptyFrameException ex) {
                //logger.warn(getLoggingFilename() + ":" + "Empty Frame:" + ex.getMessage());
                this.emptyFrameBytes += ID3v22Frame.FRAME_HEADER_SIZE;
            } catch (InvalidFrameIdentifierException ifie) {
                //logger.info(getLoggingFilename() + ":" + "Invalid Frame Identifier:" + ifie.getMessage());
                this.invalidFrames++;
                //Dont try and find any more frames
                break;
            }
            //Problem trying to find frame
            catch (InvalidFrameException ife) {
                //logger.warn(getLoggingFilename() + ":" + "Invalid Frame:" + ife.getMessage());
                this.invalidFrames++;
                //Dont try and find any more frames
                break;
            }
            //Failed reading frame but may just have invalid data but correct length so lets carry on
            //in case we can read the next frame
            catch (InvalidDataTypeException idete) {
                //logger.warn(getLoggingFilename() + ":Corrupt Frame:" + idete.getMessage());
                this.invalidFrames++;
                continue;
            }
        }
    }

    /**
     * This is used when we need to translate a single frame into multiple frames,
     * currently required for TDRC frames.
     *
     * @param frame
     */
    //TODO will overwrite any existing TYER or TIME frame, do we ever want multiples of these
    protected void translateFrame(AbstractID3v2Frame frame) {
        FrameBodyTDRC tmpBody = (FrameBodyTDRC) frame.getBody();
        ID3v22Frame newFrame;
        if (tmpBody.getYear().length() != 0) {
            //Create Year frame (v2.2 id,but uses v2.3 body)
            newFrame = new ID3v22Frame(ID3v22Frames.FRAME_ID_V2_TYER);
            ((AbstractFrameBodyTextInfo) newFrame.getBody()).setText(tmpBody.getYear());
            frameMap.put(newFrame.getIdentifier(), newFrame);
        }
        if (tmpBody.getTime().length() != 0) {
            //Create Time frame (v2.2 id,but uses v2.3 body)
            newFrame = new ID3v22Frame(ID3v22Frames.FRAME_ID_V2_TIME);
            ((AbstractFrameBodyTextInfo) newFrame.getBody()).setText(tmpBody.getTime());
            frameMap.put(newFrame.getIdentifier(), newFrame);
        }
    }


    /**
     * Write the ID3 header to the ByteBuffer.
     * <p/>
     *
     * @param padding
     * @param size
     * @return ByteBuffer
     * @throws IOException
     */
    private ByteBuffer writeHeaderToBuffer(int padding, int size) throws IOException {
        compression = false;

        //Create Header Buffer
        ByteBuffer headerBuffer = ByteBuffer.allocate(TAG_HEADER_LENGTH);

        //TAGID
        headerBuffer.put(TAG_ID);
        //Major Version
        headerBuffer.put(getMajorVersion());
        //Minor Version
        headerBuffer.put(getRevision());

        //Flags
        byte flags = (byte) 0;
        if (unsynchronization) {
            flags |= (byte) MASK_V22_UNSYNCHRONIZATION;
        }
        if (compression) {
            flags |= (byte) MASK_V22_COMPRESSION;
        }

        headerBuffer.put(flags);

        //Size As Recorded in Header, don't include the main header length
        headerBuffer.put(ID3SyncSafeInteger.valueToBuffer(padding + size));
        headerBuffer.flip();
        return headerBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(File file, long audioStartLocation) throws IOException {
        setLoggingFilename(file.getName());
        //logger.info("Writing tag to file:" + getLoggingFilename());

        // Write Body Buffer
        byte[] bodyByteBuffer = writeFramesToBuffer().toByteArray();

        // Unsynchronize if option enabled and unsync required
        unsynchronization = TagOptionSingleton.getInstance().isUnsyncTags() && ID3Unsynchronization.requiresUnsynchronization(bodyByteBuffer);
        if (isUnsynchronization()) {
            bodyByteBuffer = ID3Unsynchronization.unsynchronize(bodyByteBuffer);
            //logger.info(getLoggingFilename() + ":bodybytebuffer:sizeafterunsynchronisation:" + bodyByteBuffer.length);
        }

        int sizeIncPadding = calculateTagSize(bodyByteBuffer.length + TAG_HEADER_LENGTH, (int) audioStartLocation);
        int padding = sizeIncPadding - (bodyByteBuffer.length + TAG_HEADER_LENGTH);
        //logger.info(getLoggingFilename() + ":Current audiostart:" + audioStartLocation);
        //logger.info(getLoggingFilename() + ":Size including padding:" + sizeIncPadding);
        //logger.info(getLoggingFilename() + ":Padding:" + padding);

        ByteBuffer headerBuffer = writeHeaderToBuffer(padding, bodyByteBuffer.length);
        writeBufferToFile(file, headerBuffer, bodyByteBuffer, padding, sizeIncPadding, audioStartLocation);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WritableByteChannel channel) throws IOException {
        //logger.info(getLoggingFilename() + ":Writing tag to channel");

        byte[] bodyByteBuffer = writeFramesToBuffer().toByteArray();
        //logger.info(getLoggingFilename() + ":bodybytebuffer:sizebeforeunsynchronisation:" + bodyByteBuffer.length);

        //Unsynchronize if option enabled and unsync required
        unsynchronization = TagOptionSingleton.getInstance().isUnsyncTags() && ID3Unsynchronization.requiresUnsynchronization(bodyByteBuffer);
        if (isUnsynchronization()) {
            bodyByteBuffer = ID3Unsynchronization.unsynchronize(bodyByteBuffer);
            //logger.info(getLoggingFilename() + ":bodybytebuffer:sizeafterunsynchronisation:" + bodyByteBuffer.length);
        }
        ByteBuffer headerBuffer = writeHeaderToBuffer(0, bodyByteBuffer.length);

        channel.write(headerBuffer);
        channel.write(ByteBuffer.wrap(bodyByteBuffer));
    }

    public void createStructure() {
        MP3File.getStructureFormatter().openHeadingElement(TYPE_TAG, getIdentifier());

        super.createStructureHeader();

        //Header
        MP3File.getStructureFormatter().openHeadingElement(TYPE_HEADER, "");
        MP3File.getStructureFormatter().addElement(TYPE_COMPRESSION, this.compression);
        MP3File.getStructureFormatter().addElement(TYPE_UNSYNCHRONISATION, this.unsynchronization);
        MP3File.getStructureFormatter().closeHeadingElement(TYPE_HEADER);
        //Body
        super.createStructureBody();

        MP3File.getStructureFormatter().closeHeadingElement(TYPE_TAG);
    }

    /**
     * @return is tag unsynchronized
     */
    public boolean isUnsynchronization() {
        return unsynchronization;
    }

    /**
     * @return is tag compressed
     */
    public boolean isCompression() {
        return compression;
    }

    /**
     * Create Frame
     *
     * @param id frameid
     * @return
     */
    public ID3v22Frame createFrame(String id) {
        return new ID3v22Frame(id);
    }


    /**
     * Create Frame for Id3 Key
     * <p/>
     * Only textual data supported at the moment, should only be used with frames that
     * support a simple string argument.
     *
     * @param id3Key
     * @param value
     * @return
     * @throws KeyNotFoundException
     * @throws FieldDataInvalidException
     */
    public TagField createField(ID3v22FieldKey id3Key, String value) throws KeyNotFoundException, FieldDataInvalidException {
        if (id3Key == null) {
            throw new KeyNotFoundException();
        }
        return super.doCreateTagField(new FrameAndSubId(id3Key.getFrameId(), id3Key.getSubId()), value);
    }

    /**
     * Retrieve the first value that exists for this id3v22key
     *
     * @param id3v22FieldKey
     * @return
     * @throws org.jaudiotagger.tag.KeyNotFoundException
     *
     */
    public String getFirst(ID3v22FieldKey id3v22FieldKey) throws KeyNotFoundException {
        if (id3v22FieldKey == null) {
            throw new KeyNotFoundException();
        }

        FrameAndSubId frameAndSubId = new FrameAndSubId(id3v22FieldKey.getFrameId(), id3v22FieldKey.getSubId());
        if (id3v22FieldKey == ID3v22FieldKey.TRACK) {
            AbstractID3v2Frame frame = getFirstField(frameAndSubId.getFrameId());
            return String.valueOf(((FrameBodyTRCK) frame.getBody()).getTrackNo());
        } else if (id3v22FieldKey == ID3v22FieldKey.TRACK_TOTAL) {
            AbstractID3v2Frame frame = getFirstField(frameAndSubId.getFrameId());
            return String.valueOf(((FrameBodyTRCK) frame.getBody()).getTrackTotal());
        } else if (id3v22FieldKey == ID3v22FieldKey.DISC_NO) {
            AbstractID3v2Frame frame = getFirstField(frameAndSubId.getFrameId());
            return String.valueOf(((FrameBodyTPOS) frame.getBody()).getDiscNo());
        } else if (id3v22FieldKey == ID3v22FieldKey.DISC_TOTAL) {
            AbstractID3v2Frame frame = getFirstField(frameAndSubId.getFrameId());
            return String.valueOf(((FrameBodyTPOS) frame.getBody()).getDiscTotal());
        } else {
            return super.doGetValueAtIndex(frameAndSubId, 0);
        }
    }

    /**
     * Delete fields with this id3v22FieldKey
     *
     * @param id3v22FieldKey
     * @throws org.jaudiotagger.tag.KeyNotFoundException
     *
     */
    public void deleteField(ID3v22FieldKey id3v22FieldKey) throws KeyNotFoundException {
        if (id3v22FieldKey == null) {
            throw new KeyNotFoundException();
        }
        super.doDeleteTagField(new FrameAndSubId(id3v22FieldKey.getFrameId(), id3v22FieldKey.getSubId()));
    }

    /**
     * Delete fields with this (frame) id
     *
     * @param id
     */
    public void deleteField(String id) {
        super.doDeleteTagField(new FrameAndSubId(id, null));
    }

    protected FrameAndSubId getFrameAndSubIdFromGenericKey(FieldKey genericKey) {
        ID3v22FieldKey id3v22FieldKey = ID3v22Frames.getInstanceOf().getId3KeyFromGenericKey(genericKey);
        if (id3v22FieldKey == null) {
            throw new KeyNotFoundException();
        }
        return new FrameAndSubId(id3v22FieldKey.getFrameId(), id3v22FieldKey.getSubId());
    }

    protected ID3Frames getID3Frames() {
        return ID3v22Frames.getInstanceOf();
    }

    /**
     * @return comparator used to order frames in preffrred order for writing to file
     *         so that most important frames are written first.
     */
    public Comparator getPreferredFrameOrderComparator() {
        return ID3v22PreferredFrameOrderComparator.getInstanceof();
    }

    /**
     * {@inheritDoc}
     */
    public List<Artwork> getArtworkList() {
        List<TagField> coverartList = getFields(FieldKey.COVER_ART);
        List<Artwork> artworkList = new ArrayList<Artwork>(coverartList.size());

        for (TagField next : coverartList) {
            FrameBodyPIC coverArt = (FrameBodyPIC) ((AbstractID3v2Frame) next).getBody();
            Artwork artwork = new Artwork();
            artwork.setMimeType(ImageFormats.getMimeTypeForFormat(coverArt.getFormatType()));
            artwork.setPictureType(coverArt.getPictureType());
            artwork.setBinaryData(coverArt.getImageData());
            artworkList.add(artwork);
        }
        return artworkList;
    }

    /**
     * {@inheritDoc}
     */
    public TagField createField(Artwork artwork) throws FieldDataInvalidException {
        AbstractID3v2Frame frame = createFrame(getFrameAndSubIdFromGenericKey(FieldKey.COVER_ART).getFrameId());
        FrameBodyPIC body = (FrameBodyPIC) frame.getBody();
        body.setObjectValue(DataTypes.OBJ_PICTURE_DATA, artwork.getBinaryData());
        body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, artwork.getPictureType());
        body.setObjectValue(DataTypes.OBJ_IMAGE_FORMAT, ImageFormats.getFormatForMimeType(artwork.getMimeType()));
        body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "");
        return frame;
    }

    public TagField createArtworkField(byte[] data, String mimeType) {
        AbstractID3v2Frame frame = createFrame(getFrameAndSubIdFromGenericKey(FieldKey.COVER_ART).getFrameId());
        FrameBodyPIC body = (FrameBodyPIC) frame.getBody();
        body.setObjectValue(DataTypes.OBJ_PICTURE_DATA, data);
        body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID);
        body.setObjectValue(DataTypes.OBJ_IMAGE_FORMAT, ImageFormats.getFormatForMimeType(mimeType));
        body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "");
        return frame;
    }
}

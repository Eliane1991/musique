/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphael Slinckx <raphael@slinckx.net>
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
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
package org.jaudiotagger.audio.ogg.util;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.logging.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * Read encoding info, only implemented for vorbis streams
 */
public class OggInfoReader {
    // Logger Object
    public static Logger logger = LoggerFactory.getLogger("org.jaudiotagger.audio.ogg.atom");

    public GenericAudioHeader read(RandomAccessFile raf) throws CannotReadException, IOException {
        GenericAudioHeader info = new GenericAudioHeader();
//        //logger.info("Started");
        long oldPos;

        //Check start of file does it have Ogg pattern
        byte[] b = new byte[OggPageHeader.CAPTURE_PATTERN.length];
        raf.read(b);
        if (!(Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN))) {
            throw new CannotReadException(ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(new String(b)));
        }

        //Now work backwards from file looking for the last ogg page, it reads the granule position for this last page
        //which must be set.
        //TODO should do buffering to cut down the number of file reads
        raf.seek(0);
        double pcmSamplesNumber = -1;
        raf.seek(raf.length() - 2);
        while (raf.getFilePointer() >= 4) {
            if (raf.read() == OggPageHeader.CAPTURE_PATTERN[3]) {
                raf.seek(raf.getFilePointer() - OggPageHeader.FIELD_CAPTURE_PATTERN_LENGTH);
                byte[] ogg = new byte[3];
                raf.readFully(ogg);
                if (ogg[0] == OggPageHeader.CAPTURE_PATTERN[0] && ogg[1] == OggPageHeader.CAPTURE_PATTERN[1] && ogg[2] == OggPageHeader.CAPTURE_PATTERN[2]) {
                    raf.seek(raf.getFilePointer() - 3);

                    oldPos = raf.getFilePointer();
                    raf.seek(raf.getFilePointer() + OggPageHeader.FIELD_PAGE_SEGMENTS_POS);
                    int pageSegments = raf.readByte() & 0xFF; //Unsigned
                    raf.seek(oldPos);

                    b = new byte[OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments];
                    raf.readFully(b);

                    OggPageHeader pageHeader = new OggPageHeader(b);
                    raf.seek(0);
                    pcmSamplesNumber = pageHeader.getAbsoluteGranulePosition();
                    break;
                }
            }
            raf.seek(raf.getFilePointer() - 2);
        }

        if (pcmSamplesNumber == -1) {
            //According to spec a value of -1 indicates no packet finished on this page, this should not occur
            throw new CannotReadException(ErrorMessage.OGG_VORBIS_NO_SETUP_BLOCK.getMsg());
        }

        //1st page = Identification Header
        OggPageHeader pageHeader = OggPageHeader.read(raf);
        byte[] vorbisData = new byte[pageHeader.getPageLength()];
        raf.read(vorbisData);
        VorbisIdentificationHeader vorbisIdentificationHeader = new VorbisIdentificationHeader(vorbisData);

        //Map to generic encodingInfo
        info.setTotalSamples((long) pcmSamplesNumber);
        info.setPreciseLength((double) (pcmSamplesNumber / vorbisIdentificationHeader.getSamplingRate()));
        info.setChannelNumber(vorbisIdentificationHeader.getChannelNumber());
        info.setSamplingRate(vorbisIdentificationHeader.getSamplingRate());
        info.setEncodingType(vorbisIdentificationHeader.getEncodingType());
        info.setExtraEncodingInfos("");

        //TODO this calculation should be done within identification header
        if (vorbisIdentificationHeader.getNominalBitrate() != 0 && vorbisIdentificationHeader.getMaxBitrate() == vorbisIdentificationHeader.getNominalBitrate() && vorbisIdentificationHeader.getMinBitrate() == vorbisIdentificationHeader.getNominalBitrate()) {
            //CBR (in kbps)
            info.setBitrate(vorbisIdentificationHeader.getNominalBitrate() / 1000);
            info.setVariableBitRate(false);
        } else if (vorbisIdentificationHeader.getNominalBitrate() != 0 && vorbisIdentificationHeader.getMaxBitrate() == 0 && vorbisIdentificationHeader.getMinBitrate() == 0) {
            //Average vbr (in kpbs)
            info.setBitrate(vorbisIdentificationHeader.getNominalBitrate() / 1000);
            info.setVariableBitRate(true);
        } else {
            //TODO need to remove comment from raf.getLength()
            info.setBitrate(computeBitrate(info.getTrackLength(), raf.length()));
            info.setVariableBitRate(true);
        }
//        //logger.info("Finished");
        return info;
    }

    private int computeBitrate(int length, long size) {
        return (int) ((size / 1000) * 8 / length);
    }
}


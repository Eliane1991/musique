package org.jaudiotagger.tag.datatype;

import org.jaudiotagger.tag.InvalidDataTypeException;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

/**
 * Represents a String whose size is determined by finding of a null character at the end of the String.
 * <p/>
 * The String itself might be of length zero (i.e just consist of the null character). The String will be encoded based
 * upon the text encoding of the frame that it belongs to.
 */
public class TextEncodedStringNullTerminated extends AbstractString {
    /**
     * Creates a new TextEncodedStringNullTerminated datatype.
     *
     * @param identifier identifies the frame type
     * @param frameBody
     */
    public TextEncodedStringNullTerminated(String identifier, AbstractTagFrameBody frameBody) {
        super(identifier, frameBody);
    }

    /**
     * Creates a new TextEncodedStringNullTerminated datatype, with value
     *
     * @param identifier
     * @param frameBody
     * @param value
     */
    public TextEncodedStringNullTerminated(String identifier, AbstractTagFrameBody frameBody, String value) {
        super(identifier, frameBody, value);
    }

    public TextEncodedStringNullTerminated(TextEncodedStringNullTerminated object) {
        super(object);
    }

    public boolean equals(Object obj) {
        return obj instanceof TextEncodedStringNullTerminated && super.equals(obj);
    }

    /**
     * Read a string from buffer upto null character (if exists)
     * <p/>
     * Must take into account the text encoding defined in the Encoding Object
     * ID3 Text Frames often allow multiple strings seperated by the null char
     * appropriate for the encoding.
     *
     * @param arr    this is the buffer for the frame
     * @param offset this is where to start reading in the buffer for this field
     */
    public void readByteArray(byte[] arr, int offset) throws InvalidDataTypeException {
        if (offset >= arr.length) {
            throw new InvalidDataTypeException("Unable to find null terminated string");
        }
        int bufferSize;
//
        //logger.infor("Reading from array starting from offset:" + offset);
        int size;

        //Get the Specified Decoder
        String charSetName = getTextEncodingCharSet();
        CharsetDecoder decoder = Charset.forName(charSetName).newDecoder();

        //We only want to load up to null terminator, data after this is part of different
        //field and it may not be possible to decode it so do the check before we do
        //do the decoding,encoding dependent.
        ByteBuffer buffer = ByteBuffer.wrap(arr, offset, arr.length - offset);
        int endPosition = 0;

        //Latin-1 and UTF-8 strings are terminated by a single-byte null,
        //while UTF-16 and its variants need two bytes for the null terminator.
        final boolean nullIsOneByte = (charSetName.equals(TextEncoding.CHARSET_ISO_8859_1) || charSetName.equals(TextEncoding.CHARSET_UTF_8));

        boolean isNullTerminatorFound = false;
        while (buffer.hasRemaining()) {
            byte nextByte = buffer.get();
            if (nextByte == 0x00) {
                if (nullIsOneByte) {
                    buffer.mark();
                    buffer.reset();
                    endPosition = buffer.position() - 1;
//                    logger.info("Null terminator found starting at:" + endPosition);

                    isNullTerminatorFound = true;
                    break;
                } else {
                    // Looking for two-byte null
                    if (buffer.hasRemaining()) {
                        nextByte = buffer.get();
                        if (nextByte == 0x00) {
                            buffer.mark();
                            buffer.reset();
                            endPosition = buffer.position() - 2;
//                            logger.info("UTF16:Null terminator found starting  at:" + endPosition);
                            isNullTerminatorFound = true;
                            break;
                        } else {
                            //Nothing to do, we have checked 2nd value of pair it was not a null terminator
                            //so will just start looking again in next invocation of loop
                        }
                    } else {
                        buffer.mark();
                        buffer.reset();
                        endPosition = buffer.position() - 1;
                        //logger.warn("UTF16:Should be two null terminator marks but only found one starting at:" + endPosition);

                        isNullTerminatorFound = true;
                        break;
                    }
                }
            } else {
                //If UTF16, we should only be looking on 2 byte boundaries
                if (!nullIsOneByte) {
                    if (buffer.hasRemaining()) {
                        buffer.get();
                    }
                }
            }
        }

        if (!isNullTerminatorFound) {
            throw new InvalidDataTypeException("Unable to find null terminated string");
        }
//

        //logger.info("End Position is:" + endPosition + "Offset:" + offset);

        //Set Size so offset is ready for next field (includes the null terminator)
        size = endPosition - offset;
        size++;
        if (!nullIsOneByte) {
            size++;
        }
        setSize(size);

        //Decode buffer if runs into problems should throw exception which we
        //catch and then set value to empty string. (We don't read the null terminator
        //because we dont want to display this)
        bufferSize = endPosition - offset;
//        logger.info("Text size is:" + bufferSize);
        if (bufferSize == 0) {
            value = "";
        } else {
            //Decode sliced inBuffer
            ByteBuffer inBuffer = ByteBuffer.wrap(arr, offset, bufferSize).slice();
            CharBuffer outBuffer = CharBuffer.allocate(bufferSize);
            decoder.reset();
            CoderResult coderResult = decoder.decode(inBuffer, outBuffer, true);
            if (coderResult.isError()) {
                //logger.warn("Problem decoding text encoded null terminated string:" + coderResult.toString());
            }
            decoder.flush(outBuffer);
            outBuffer.flip();
            value = outBuffer.toString();
        }
        //Set Size so offset is ready for next field (includes the null terminator)
        //logger.info("Read NullTerminatedString:" + value + " size inc terminator:" + size);
    }

    /**
     * Write String into byte array, adding a null character to the end of the String
     *
     * @return the data as a byte array in format to write to file
     */
    public byte[] writeByteArray() {
        //logger.info("Writing NullTerminatedString." + value);
        byte[] data;
        //Write to buffer using the CharSet defined by getTextEncodingCharSet()
        //Add a null terminator which will be encoded based on encoding.
        try {
            String charSetName = getTextEncodingCharSet();
            if (charSetName.equals(TextEncoding.CHARSET_UTF_16)) {
                charSetName = TextEncoding.CHARSET_UTF_16_ENCODING_FORMAT;
                CharsetEncoder encoder = Charset.forName(charSetName).newEncoder();
                //Note remember LE BOM is ff fe but this is handled by encoder Unicode char is fe ff
                ByteBuffer bb = encoder.encode(CharBuffer.wrap('\ufeff' + (String) value + '\0'));
                data = new byte[bb.limit()];
                bb.get(data, 0, bb.limit());
            } else {
                CharsetEncoder encoder = Charset.forName(charSetName).newEncoder();
                ByteBuffer bb = encoder.encode(CharBuffer.wrap((String) value + '\0'));
                data = new byte[bb.limit()];
                bb.get(data, 0, bb.limit());
            }
        }
        //Should never happen so if does throw a RuntimeException
        catch (CharacterCodingException ce) {
            logger.error(ce.getMessage());
            throw new RuntimeException(ce);
        }
        setSize(data.length);
        return data;
    }

    protected String getTextEncodingCharSet() {
        byte textEncoding = this.getBody().getTextEncoding();
        String charSetName = TextEncoding.getInstanceOf().getValueForId(textEncoding);
        //logger.info("text encoding:" + textEncoding + " charset:" + charSetName);
        return charSetName;
    }
}

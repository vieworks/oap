/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.storage.mongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.SneakyThrows;
import lombok.val;
import oap.json.Binder;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;

import java.util.function.Function;

/**
 * Created by igor.petrenko on 13.12.2017.
 */
public class JsonCodec<T> implements Codec<T> {
    private final DocumentCodec documentCodec;
    private final Class<T> clazz;
    private final Function<T, String> idFunc;
    private ObjectWriter fileWriter;
    private ObjectReader fileReader;

    public JsonCodec( TypeReference<T> tr, Class<T> clazz, Function<T, String> idFunc ) {
        this.clazz = clazz;
        this.idFunc = idFunc;
        documentCodec = new DocumentCodec();
        fileReader = Binder.json.readerFor( tr );
        fileWriter = Binder.json.writerFor( tr );
    }

    @SneakyThrows
    @Override
    public T decode( BsonReader bsonReader, DecoderContext decoderContext ) {
        val doc = documentCodec.decode( bsonReader, decoderContext );
        doc.remove( "_id" );

        return fileReader.readValue( Binder.json.marshal( doc ) );
    }

    @SneakyThrows
    @Override
    public void encode( BsonWriter bsonWriter, T data, EncoderContext encoderContext ) {
        val doc = Document.parse( fileWriter.writeValueAsString( data ) );

        val id = idFunc.apply( data );

        doc.put( "_id", id );

        documentCodec.encode( bsonWriter, doc, encoderContext );
    }

    @Override
    public Class<T> getEncoderClass() {
        return clazz;
    }
}

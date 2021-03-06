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

package oap.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MutableObjectModule {
    public static class MutableObjectSerializer extends StdSerializer<MutableObject<?>> {
        public MutableObjectSerializer( JavaType type ) {
            super( type );
        }

        @Override
        public void serialize( MutableObject value, JsonGenerator gen, SerializerProvider serializers ) throws IOException {
            gen.writeObject( value.getValue() );
        }
    }

    public static class MutableObjectSerializers extends Serializers.Base {
        @Override
        public JsonSerializer<?> findSerializer( SerializationConfig config, JavaType type, BeanDescription beanDesc ) {
            Class<?> raw = type.getRawClass();
            if( MutableObject.class.isAssignableFrom( raw ) ) {
                return new MutableObjectSerializer( type );
            }

            return super.findSerializer( config, type, beanDesc );
        }
    }

    public static class MutableObjectDeserializer extends StdDeserializer<MutableObject<?>> {
        private final JavaType refType;

        protected MutableObjectDeserializer( JavaType valueType, JavaType refType ) {
            super( valueType );
            this.refType = refType;
        }

        @Override
        public MutableObject<?> deserialize( JsonParser p, DeserializationContext ctxt ) throws IOException {

            try {
                MutableObject<?> mutableObject = ( MutableObject<?> ) _valueClass.getDeclaredConstructor().newInstance();

                mutableObject.setValue( ctxt.readValue( p, refType ) );

                return mutableObject;
            } catch( InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e ) {
                throw ctxt.instantiationException( _valueClass, e );
            }
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public MutableObject<?> deserialize( JsonParser p, DeserializationContext ctxt, MutableObject intoValue ) throws IOException {
            intoValue.setValue( ctxt.readValue( p, refType ) );

            return intoValue;
        }
    }

    public static class MutableObjectDeserializers extends Deserializers.Base {
        @Override
        public JsonDeserializer<?> findBeanDeserializer( JavaType type, DeserializationConfig config,
                                                         BeanDescription beanDesc ) throws JsonMappingException {
            Class<?> raw = type.getRawClass();
            if( MutableObject.class.isAssignableFrom( raw ) ) {
                Type[] actualTypeArguments = ( ( ParameterizedType ) raw.getGenericSuperclass() ).getActualTypeArguments();
                JavaType refType = config.constructType( ( Class<?> ) actualTypeArguments[0] );
                return new MutableObjectDeserializer( type, refType );
            }

            return super.findBeanDeserializer( type.getReferencedType(), config, beanDesc );
        }
    }
}


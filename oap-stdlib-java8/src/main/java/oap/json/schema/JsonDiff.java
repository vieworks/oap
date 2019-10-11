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

package oap.json.schema;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import oap.json.Binder;
import oap.json.schema.validator.array.ArraySchemaAST;
import oap.json.schema.validator.object.ObjectSchemaAST;
import oap.reflect.TypeRef;
import oap.util.Lists;
import oap.util.Sets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.CollectionUtils.subtract;

public class JsonDiff {
    private final ArrayList<Line> diff;

    private JsonDiff( ArrayList<Line> diff ) {
        this.diff = diff;
    }

    public static JsonDiff diff( String oldJson, String newJson, SchemaAST schema ) {
        val result = new ArrayList<Line>();

        val to = Binder.json.unmarshal( new TypeRef<Map<String, Object>>() {
        }, newJson );
        val from = Binder.json.unmarshal( new TypeRef<Map<String, Object>>() {
        }, oldJson );

        diff( "", schema, result, to, from );

        return new JsonDiff( result );
    }

    private static void diff( String prefix, SchemaAST schema, ArrayList<Line> result, Object to, Object from ) {
        if( schema instanceof ObjectSchemaAST ) {
            diffObject( prefix, ( ObjectSchemaAST ) schema, result, to, from );
        } else if( schema instanceof ArraySchemaAST ) {
            diffArray( prefix, ( ArraySchemaAST ) schema, result, to, from );
        } else {
            diffField( prefix, schema, result, to, from );
        }
    }

    private static void diffField( String prefix, SchemaAST schema, ArrayList<Line> result, Object to, Object from ) {
        if( !Objects.equals( to, from ) ) {
            result.add( new Line(
                prefix,
                toLineType( schema ),
                Optional.ofNullable( from ).map( Binder.json::marshal ),
                Optional.ofNullable( to ).map( Binder.json::marshal )
            ) );
        }
    }

    private static void diffArray( String prefix, ArraySchemaAST schema, ArrayList<Line> result, Object to, Object from ) {
        if( !( to instanceof List ) || !( from instanceof List ) )
            throw new IllegalArgumentException( prefix + ": invalid json" );
        val toList = ( List<?> ) to;
        val fromList = ( List<?> ) from;

        val diffDel = subtract( fromList, toList );
        val diffAdd = subtract( toList, fromList );

        val items = schema.items;
        if( items instanceof ObjectSchemaAST ) {
            val idField = schema.idField
                .orElseThrow( () -> new IllegalArgumentException( prefix + ": schema: id field is required" ) );

            val added = unique( toList, fromList, idField );
            val removed = unique( fromList, toList, idField );

            for( int i = 0; i < added.size(); i++ ) {
                val item = added.get( i );

                val id = isIndex( idField ) ? fromList.size() : getId( idField, item );
                diffField( prefixWithIndex( prefix, id ), items, result, item, null );
            }

            for( int i = 0; i < removed.size(); i++ ) {
                val item = removed.get( i );

                val id = isIndex( idField ) ? toList.size() : getId( idField, item );

                diffField( prefixWithIndex( prefix, id ), items, result, null, item );
            }


            if( isIndex( idField ) ) {
                for( int i = 0; i < min( fromList.size(), toList.size() ); i++ ) {
                    diff( prefixWithIndex( prefix, i ), items, result, toList.get( i ), fromList.get( i ) );
                }
            } else {
                for( val fromItem : fromList ) {
                    val fromItemMap = ( Map ) fromItem;
                    val id = fromItemMap.get( idField );
                    if( id == null )
                        throw new IllegalArgumentException( prefix + ": id field " + idField + ": not found" );

                    toList
                        .stream()
                        .filter( toItem -> {
                            val toItemMap = ( Map ) toItem;
                            val toId = toItemMap.get( idField );

                            return Objects.equals( toId, id );
                        } )
                        .findAny()
                        .ifPresent( toItem -> diff( prefixWithIndex( prefix, id ), items, result, toItem, fromItemMap ) );
                }
            }
        } else if( items instanceof ArraySchemaAST ) {
            throw new IllegalArgumentException( prefix + ": sub-array" );
        } else {

            diffField( prefix, schema, result, diffAdd.isEmpty() ? null : diffAdd, diffDel.isEmpty() ? null : diffDel );
        }
    }

    private static boolean isIndex( String idField ) {
        return "{index}".equals( idField );
    }

    private static String prefixWithIndex( String prefix, Object id ) {
        return prefix.isEmpty() ? "[" + id + "]" : prefix + "[" + id + "]";
    }

    private static List unique( List<?> to, List<?> from, String idField ) {
        if( isIndex( idField ) ) {
            if( to.size() <= from.size() ) return emptyList();
            return to.subList( from.size(), to.size() );
        } else {
            final Set<Object> ids2 = Sets.map( from, i -> getId( idField, i ) );

            return Lists.filter( to, i -> {
                val id = getId( idField, i );

                return !ids2.contains( id );
            } );
        }
    }

    private static Object getId( String idField, Object i ) {
        return ( ( Map<?, ?> ) i ).get( idField );
    }

    private static void diffObject( String prefix, ObjectSchemaAST schema, ArrayList<Line> result, Object to, Object from ) {

        val toMap = ( Map ) to;
        val fromMap = ( Map ) from;

        for( val child : schema.properties.entrySet() ) {
            val property = child.getKey();
            val fromProperty = fromMap.get( property );
            val toProperty = toMap.get( property );

            val newPrefix = prefix.length() > 0 ? prefix + "." + property : property;

            val schemaAST = child.getValue();

            if( ( fromProperty == null && toProperty != null ) || ( toProperty == null && fromProperty != null ) ) {
                diffField( newPrefix, schemaAST, result, toProperty, fromProperty );
            } else if( fromProperty != null ) {
                diff( newPrefix, schemaAST, result, toProperty, fromProperty );
            }
        }
    }

    private static Line.LineType toLineType( SchemaAST schema ) {
        if( schema instanceof ObjectSchemaAST ) return Line.LineType.OBJECT;
        else if( schema instanceof ArraySchemaAST ) return Line.LineType.ARRAY;
        return Line.LineType.FIELD;
    }

    public List<Line> getDiff() {
        return unmodifiableList( diff );
    }

    @EqualsAndHashCode
    @ToString
    public static class Line implements Serializable {
        private static final long serialVersionUID = 5804735144221122177L;

        public final String path;
        public final Optional<String> oldValue;
        public final LineType lineType;
        public final Optional<String> newValue;

        public Line( String path, LineType lineType, Optional<String> oldValue, Optional<String> newValue ) {
            this.path = path;
            this.lineType = lineType;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }

        public enum LineType {
            ARRAY, FIELD, OBJECT
        }
    }
}
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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import oap.reflect.TypeRef;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Stream;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class JsonPatch {
    public static Map<String, Object> patchObject( Object dest, String draftJson ) {
        Map<String, Object> destSchema = Binder.json.unmarshal( new TypeReference<Map<String, Object>>() {}, dest );
        Map<String, Object> sourceSchema = Binder.json.unmarshal( new TypeRef<Map<String, Object>>() {}, draftJson );

        return deepMerge( destSchema, sourceSchema );
    }

    private static Map<String, Object> deepMerge( Map<String, Object> dest, Map<String, Object> source ) {
        return Stream.of( dest, source )
            .flatMap( map -> map.entrySet().stream() )
            .filter( e -> e.getValue() != null )
            .collect( Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                ( v1, v2 ) -> {
                    if( ( v1 instanceof List && !( ( List ) v1 ).isEmpty() && ( ( List ) v1 ).get( 0 ) instanceof Map )
                        && ( v2 instanceof List && !( ( List ) v2 ).isEmpty() && ( ( List ) v2 ).get( 0 ) instanceof Map ) ) {

                        Iterator iteratorDest = ( ( List ) v1 ).iterator();
                        Iterator iteratorSource = ( ( List ) v2 ).iterator();

                        boolean destPresent = iteratorDest.hasNext();
                        boolean sourcePresent = iteratorSource.hasNext();

                        List<Map<String, Object>> result = Lists.empty();

                        while( destPresent || sourcePresent ) {
                            Map<String, Object> d = destPresent ? ( Map ) iteratorDest.next() : Maps.empty();
                            Map<String, Object> s = sourcePresent ? ( Map ) iteratorSource.next() : Maps.empty();

                            result.add( deepMerge( d, s ) );
                            destPresent = iteratorDest.hasNext();
                            sourcePresent = iteratorSource.hasNext();
                        }
                        return result;
                    } else {
                        return v2;
                    }
                }
            ) );
    }
}

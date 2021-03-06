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

package oap.dictionary;

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.Resources;
import oap.util.Maps;
import oap.util.Stream;
import oap.util.Try;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;
import static oap.util.Pair.__;

@Slf4j
public class Dictionaries {
    public static final String DEFAULT_PATH = "/opt/oap-dictionary";
    private static final Map<String, URL> dictionaries = new HashMap<>();
    private static final ConcurrentHashMap<String, DictionaryRoot> cache = new ConcurrentHashMap<>();

    private static synchronized void load() {
        if( dictionaries.isEmpty() ) {
            dictionaries.putAll( Stream.of( Files.fastWildcard( DEFAULT_PATH, "*.json" ).stream() )
                .concat( Files.fastWildcard( DEFAULT_PATH, "*.conf" ).stream() )
                .concat( Files.fastWildcard( DEFAULT_PATH, "*.yaml" ).stream() )
                .map( Try.map( p -> p.toUri().toURL() ) )
                .mapToPairs( r -> __( Files.nameWithoutExtention( r ), r ) )
                .toMap() );

            dictionaries.putAll( Stream.of( Stream.of( Resources.urls( "dictionary", "json" ) )
                .concat( Resources.urls( "dictionary", "conf" ).stream() )
                .concat( Resources.urls( "dictionary", "yaml" ).stream() )
                .collect( toList() ) )
                .mapToPairs( r -> __( Files.nameWithoutExtention( r ), r ) )
                .filter( p -> !dictionaries.containsKey( p._1 ) )
                .toMap() );

            log.info( "dictionaries: {}", dictionaries );
        }
    }

    public static Set<String> getDictionaryNames() {
        load();

        return dictionaries.keySet();
    }

    public static DictionaryRoot getDictionary( String name ) {
        return getDictionary( name, DictionaryParser.PROPERTY_ID_STRATEGY );
    }

    public static DictionaryRoot getDictionary( String name, DictionaryParser.IdStrategy idStrategy ) {
        load();

        return Maps.get( dictionaries, name )
            .map( d -> DictionaryParser.parse( d, idStrategy ) )
            .orElseThrow( () -> new DictionaryNotFoundError( name ) );
    }

    public static DictionaryRoot getCachedDictionary( String name ) {
        return cache.computeIfAbsent( name, Dictionaries::getDictionary );
    }
}

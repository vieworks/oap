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

package oap.testng;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.extern.slf4j.Slf4j;
import oap.util.Pair;
import oap.util.Strings;

import static oap.testng.EnvFixture.Scope.CLASS;
import static oap.testng.EnvFixture.Scope.METHOD;
import static oap.testng.EnvFixture.Scope.SUITE;
import static oap.util.Pair.__;

@Slf4j
public class EnvFixture implements Fixture {
    private final ListMultimap<Scope, Pair<String, Object>> properties = ArrayListMultimap.create();

    public EnvFixture() {
    }

    public EnvFixture define( String property, Object value ) {
        return define( METHOD, property, value );
    }

    public EnvFixture define( Scope scope, String property, Object value ) {
        properties.get( scope ).add( __( property, value ) );
        return this;
    }


    private void init( Scope scope ) {
        properties.get( scope ).forEach( p -> {
            String value = Strings.substitute( String.valueOf( p._2 ),
                k -> System.getenv( k ) == null ? System.getProperty( k ) : System.getenv( k ) );
            log.debug( "system property {} = {}", p._1, value );
            System.setProperty( p._1, value );
        } );
    }

    @Override
    public void beforeSuite() {
        init( SUITE );
    }

    @Override
    public void beforeClass() {
        init( CLASS );
    }

    @Override
    public void beforeMethod() {
        init( METHOD );
    }

    public enum Scope {
        METHOD, CLASS, SUITE
    }
}

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

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.val;
import oap.io.Files;
import oap.io.Resources;
import oap.util.Throwables;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log
public class Env {
    public static final String LOCALHOST;
    static final Path tmp = Paths.get( "/tmp/test" );
    public static final Path tmpRoot = tmp.resolve( "temp" + Teamcity.buildPrefix() + "_" + System.currentTimeMillis() );
    private static final ConcurrentHashMap<String, Integer> ports = new ConcurrentHashMap<>();

    static {
        System.setProperty( "oap.test.tmpdir", Env.tmpRoot.toString() );
        System.out.println( "initializing test directory " + tmpRoot );
        try {
            LOCALHOST = InetAddress.getByName( "127.0.0.1" ).getCanonicalHostName();
        } catch( UnknownHostException e ) {
            throw Throwables.propagate( e );
        }
    }

    public static String tmp( String name ) {
        return tmpPath( name ).toString();
    }

    public static URI tmpURI( String name ) {
        return tmpPath( name ).toUri();
    }

    @SneakyThrows
    public static URL tmpURL( String name ) {
        return tmpURI( name ).toURL();
    }

    public static Path tmpPath( String name ) {
        Path tmpPath = tmpRoot.resolve(
            name.startsWith( "/" ) || name.startsWith( "\\" ) ? name.substring( 1 ) : name );
        try {
            java.nio.file.Files.createDirectories( tmpPath.getParent() );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
        return tmpPath;
    }

    public static Path deployTestData( Class<?> contextClass ) {
        return deployTestData( contextClass, "" );
    }

    public static Path deployTestData( Class<?> contextClass, String name ) {
        Path to = tmpPath( name );
        Resources.filePaths( contextClass, contextClass.getSimpleName() )
            .forEach( path -> Files.copyDirectory( path, to ) );
        return to;
    }

    public static int port() {
        return port( "DEFAULT" );
    }

    public static int port( String key ) {
        return ports.computeIfAbsent( key, k -> {
            try( ServerSocket socket = new ServerSocket( 0 ) ) {
                val localPort = socket.getLocalPort();
                System.out.println( "ENV::key=" + key + "; port = " + localPort );
                return localPort;
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        } );
    }

    public static String getEnvOrDefault( String name, String defaultValue ) {
        val res = System.getenv( name );
        return res != null ? res : defaultValue;
    }

    public static void resetPorts() {
        System.out.println( "ENV::ports = []" );
        ports.clear();
    }

    @SneakyThrows
    public static void putEnv( String name, String value ) {
        try {
            Class<?> processEnvironmentClass = Class.forName( "java.lang.ProcessEnvironment" );
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField( "theEnvironment" );
            theEnvironmentField.setAccessible( true );
            val env = ( Map<Object, Object> ) theEnvironmentField.get( null );

            if( SystemUtils.IS_OS_WINDOWS ) {
                if( value == null ) {
                    env.remove( name );
                } else {
                    env.put( name, value );
                }
            } else {
                val variableClass = Class.forName( "java.lang.ProcessEnvironment$Variable" );
                val convertToVariable = variableClass.getMethod( "valueOf", String.class );
                convertToVariable.setAccessible( true );

                val valueClass = Class.forName( "java.lang.ProcessEnvironment$Value" );
                val convertToValue = valueClass.getMethod( "valueOf", String.class );
                convertToValue.setAccessible( true );

                if( value == null ) {
                    env.remove( convertToVariable.invoke( null, name ) );
                } else {
                    env.put( convertToVariable.invoke( null, name ), convertToValue.invoke( null, value ) );
                }
            }

            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField( "theCaseInsensitiveEnvironment" );
            theCaseInsensitiveEnvironmentField.setAccessible( true );
            Map<String, String> cienv = ( Map<String, String> ) theCaseInsensitiveEnvironmentField.get( null );

            if( value == null ) {
                cienv.remove( name );
            } else {
                cienv.put( name, value );
            }
        } catch( NoSuchFieldException e ) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for( Class cl : classes ) {
                if( "java.util.Collections$UnmodifiableMap".equals( cl.getName() ) ) {
                    Field field = cl.getDeclaredField( "m" );
                    field.setAccessible( true );
                    Object obj = field.get( env );
                    Map<String, String> map = ( Map<String, String> ) obj;

                    if( value == null ) {
                        // remove if null
                        map.remove( name );
                    } else {
                        map.put( name, value );
                    }
                }
            }
        }
    }
}
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
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.Resources;
import oap.util.Dates;
import org.joda.time.DateTimeUtils;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class TestDirectoryFixture implements Fixture {
    public static TestDirectoryFixture FIXTURE = new TestDirectoryFixture();
    private static final Path globalTestDirectory = Paths.get( "/tmp/test" );
    private static final Path testDirectory = globalTestDirectory().resolve( "test-" + Suite.uniqueExecutionId() );

    static {
        Files.ensureDirectory( testDirectory() );
        System.out.println( "initializing test directory " + testDirectory() );
    }

    public static Path globalTestDirectory() {
        return globalTestDirectory;
    }

    public static void deleteDirectory( Path path ) {
        try {
            Files.delete( path );
        } catch( UncheckedIOException e ) {
            Files.wildcard( globalTestDirectory(), "**/*" ).forEach( System.out::println );
            throw e;
        }
    }

    public static Path testDirectory() {
        return testDirectory;
    }

    public static Path testPath( String name ) {
        Path path = testDirectory().resolve( name.startsWith( "/" ) || name.startsWith( "\\" ) ? name.substring( 1 ) : name );
        Files.ensureFile( path );
        return path;
    }

    public static Path deployTestData( Class<?> contextClass ) {
        return deployTestData( contextClass, "" );
    }

    public static Path deployTestData( Class<?> contextClass, String name ) {
        Path to = testPath( name );
        Resources.filePaths( contextClass, contextClass.getSimpleName() )
            .forEach( path -> Files.copyDirectory( path, to ) );
        return to;
    }

    @Override
    public void afterClass() {
        TestDirectoryFixture.deleteDirectory( testDirectory() );
        cleanTestDirectories();
    }

    @Override
    public void afterMethod() {
        TestDirectoryFixture.deleteDirectory( testDirectory() );
    }

    @SneakyThrows
    private void cleanTestDirectories() {
        try( var stream = java.nio.file.Files.list( globalTestDirectory() ) ) {
            stream
                .filter( path -> java.nio.file.Files.isDirectory( path ) )
                .filter( path -> Files.getLastModifiedTime( path ) < DateTimeUtils.currentTimeMillis() - Dates.h( 2 ) )
                .forEach( path -> {
                    try {
                        Files.delete( path );
                    } catch( Exception ignored ) {
                    }
                } );
        }
    }
}

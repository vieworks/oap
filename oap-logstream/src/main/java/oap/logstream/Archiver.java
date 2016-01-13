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
package oap.logstream;

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import org.joda.time.DateTimeUtils;

import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;

@Slf4j
public class Archiver implements Runnable {
    private final Path sourceDirectory;
    private final Path destinationDirectory;
    private final long safeInterval;
    private final boolean compress;
    private final String targetExtention;
    private final String mask;


    public Archiver( Path sourceDirectory, Path destinationDirectory, long safeInterval, String mask, boolean compress, String targetExtention ) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.safeInterval = safeInterval;
        this.mask = mask;
        this.compress = compress;
        this.targetExtention = targetExtention;
    }

    @Override
    public void run() {
        log.debug( "let's start packing of " + mask + " in " + sourceDirectory + " into " + destinationDirectory );

        for( Path path : Files.wildcard( sourceDirectory, mask ) )
            if( path.toFile().lastModified() < DateTimeUtils.currentTimeMillis() - safeInterval ) {
                log.debug( "archiving " + path );

                final String dotTargetExtension = targetExtention.isEmpty() ? "" : "." + targetExtention;

                Path targetFile = destinationDirectory.resolve( sourceDirectory.relativize( path ) + dotTargetExtension );

                if( compress ) {
                    Path targetTemp = destinationDirectory.resolve( sourceDirectory.relativize( path ) + dotTargetExtension + ".tmp" );
                    Files.copy( path, PLAIN, targetTemp, GZIP );
                    Files.rename( targetTemp, targetFile );
                } else {
                    Files.ensureFile( targetFile );
                    Files.rename( path, targetFile );
                }
                Files.delete( path );
            } else log.debug( "skipping (not safe yet) " + path );
        log.debug( "packing is done" );
    }
}

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

package oap.storage;

import oap.json.TypeIdFactory;
import oap.storage.Bean.BeanMigration;
import oap.storage.Bean2.Bean2Migration;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

public class FileStorageMigrationTest extends AbstractTest {
    @BeforeMethod
    @Override
    public void beforeMethod() {
        super.beforeMethod();

        TypeIdFactory.register( Bean.class, Bean.class.getName() );
        TypeIdFactory.register( Bean2.class, Bean2.class.getName() );
    }

    @Test
    public void testMigration() {
        final Path data = Env.tmpPath( "data" );
        try( FileStorage<Bean> storage1 = new FileStorage<>( data, b -> b.id, -1 ) ) {
            storage1.start();
            storage1.store( new Bean( "1" ) );
            storage1.store( new Bean( "2" ) );
        }

        assertThat( data.resolve( "1.json" ) ).exists();
        assertThat( data.resolve( "2.json" ) ).exists();

        try( FileStorage<Bean2> storage2 = new FileStorage<>( data, b -> b.id2, -1, 2, Lists.of(
            BeanMigration.class.getName(),
            Bean2Migration.class.getName()
        ) ) ) {
            storage2.start();
            assertThat( storage2.select() ).containsExactly( new Bean2( "11" ), new Bean2( "21" ) );
        }

        assertThat( data.resolve( "1.json" ) ).doesNotExist();
        assertThat( data.resolve( "2.json" ) ).doesNotExist();

        assertThat( data.resolve( "1.v1.json" ) ).doesNotExist();
        assertThat( data.resolve( "2.v1.json" ) ).doesNotExist();

        assertThat( data.resolve( "1.v2.json" ) ).exists();
        assertThat( data.resolve( "2.v2.json" ) ).exists();
    }

    @Test
    public void testStoreWithVersion() {
        final Path data = Env.tmpPath( "data" );
        try( FileStorage<Bean> storage1 = new FileStorage<>( data, b -> b.id, -1, 10, emptyList() ) ) {
            storage1.start();
            storage1.store( new Bean( "1" ) );
        }

        assertThat( contentOf( data.resolve( "1.v10.json" ).toFile()) ).contains( "\"version\":10" );
    }
}

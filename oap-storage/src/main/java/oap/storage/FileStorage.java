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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.json.Binder;
import oap.util.Stream;
import oap.util.Try;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;
import static oap.util.Maps.Collectors.toConcurrentMap;
import static oap.util.Pair.__;

@Slf4j
public class FileStorage<T> implements Storage<T>, Closeable {
    private final AtomicLong lastFSync = new AtomicLong( 0 );
    private final AtomicLong lastRSync = new AtomicLong( 0 );
    private final Storage<T> master;
    private final Scheduled rsync;
    private final Scheduled fsync;
    protected long rsyncSafeInterval = 1000;
    protected long fsyncSafeInterval = 1000;
    protected Function<T, String> identify;
    protected ConcurrentMap<String, Metadata<T>> data = new ConcurrentHashMap<>();
    private Path path;
    private List<DataListener<T>> dataListeners = new ArrayList<>();

    public FileStorage( Path path, Function<T, String> identify, long fsync, Storage<T> master, long rsync ) {
        this.path = path;
        this.identify = identify;
        load();
        if( fsync > 0 )
            this.fsync = Scheduler.scheduleWithFixedDelay( fsync, TimeUnit.MILLISECONDS, this::fsync );
        else this.fsync = null;
        this.master = master;
        if( master != null )
            this.rsync = Scheduler.scheduleWithFixedDelay( rsync, TimeUnit.MILLISECONDS, this::rsync );
        else this.rsync = null;
    }

    public FileStorage( Path path, Function<T, String> identify, long fsync ) {
        this( path, identify, fsync, null, 0 );
    }

    public FileStorage( Path path, Function<T, String> identify ) {
        this( path, identify, 60000 );
    }

    @SuppressWarnings( "unchecked" )
    protected void load() {
        path.toFile().mkdirs();
        data = Files.wildcard( path, "*.json" )
            .stream()
            .map( Try.map(
                f -> ( Metadata<T> ) Binder.json.unmarshal( new TypeReference<Metadata<T>>() {
                }, f ) ) )
            .sorted( reverseOrder() )
            .map( x -> __( x.id, x ) )
            .collect( toConcurrentMap() );
        log.info( data.size() + " object(s) loaded." );
    }

    private synchronized void fsync() {
        long current = DateTimeUtils.currentTimeMillis() - fsyncSafeInterval;
        long last = lastFSync.get();

        data.values()
            .stream()
            .filter( m -> m.modified > last )
            .forEach( Try.consume( m -> Binder.json.marshal( path.resolve( m.id + ".json" ), m ) ) );

        lastFSync.set( current );
    }

    private void rsync() {
        long current = DateTimeUtils.currentTimeMillis() - rsyncSafeInterval;
        long last = lastRSync.get();
        List<Metadata<T>> updates = master.updatedSince( last );
        log.trace( "current: {}, last: {}, to sync {}", current, last, updates.size() );
        for( Metadata<T> metadata : updates ) {
            log.debug( "rsync {}", metadata );
            data.put( metadata.id, metadata );
        }
        fireUpdated( Stream.of( updates ).map( m -> m.object ).toList() );
        lastRSync.set( current );
    }


    @Override
    public Stream<T> select() {
        return Stream.of( data.values() )
            .filter( m -> !m.deleted )
            .map( m -> m.object );
    }

    @Override
    public void store( T object ) {
        String id = this.identify.apply( object );
        synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata != null ) {
                metadata.update( object );
                metadata.deleted = false;
            }
            else {
                data.put( id, new Metadata<>( id, object ) );
            }
            fireUpdated( object );
        }
    }

    @Override
    public void store( Collection<T> objects ) {
        for( T object : objects ) {
            String id = this.identify.apply( object );
            synchronized( id.intern() ) {
                Metadata<T> metadata = data.get( id );
                if( metadata != null ) {
                    metadata.update( object );
                    metadata.deleted = false;
                }
                else {
                    data.put( id, new Metadata<>( id, object ) );
                }
            }
        }
        fireUpdated( objects );
    }

    @Override
    public T update( String id, Consumer<T> update ) {
        return update( id, update, null );
    }

    @Override
    public T update( String id, Consumer<T> update, Supplier<T> init ) {
        return update( id, update, init, true );
    }

    private T update( String id, Consumer<T> update, Supplier<T> init, boolean notification ) {
        synchronized( id.intern() ) {
            Metadata<T> m = data.get( id );
            if( m == null ) {
                if( init == null ) return null;
                T object = init.get();
                m = new Metadata<>( identify.apply( object ), object );
                data.put( m.id, m );
            } else {
                update.accept( m.object );
                m.update( m.object );
            }
            if( notification ) fireUpdated( m.object );
            return m.object;
        }
    }

    @Override
    public void update( Collection<String> ids, Consumer<T> update ) {
        update( ids, update, null );
    }

    @Override
    public void update( Collection<String> ids, Consumer<T> update, Supplier<T> init ) {
        final List<T> collect = ids.stream().map( id -> update( id, update, init, false ) ).collect( toList() );

        fireUpdated( collect );
    }

    @Override
    public Optional<T> get( String id ) {
        synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata == null || metadata.deleted ) {
                return Optional.empty();
            }
            else return Optional.of( metadata.object );
        }
    }

    @Override
    public void clear() {
        List<T> objects = new ArrayList<>();
        for( String id : data.keySet() ) remove( id ).ifPresent( m -> objects.add( m.object ) );
        fireDeleted( objects );
    }

    private Optional<Metadata<T>> remove( String id ) {
        synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata != null && !metadata.deleted ) {
                metadata.delete();
                return Optional.of( metadata );
            }
            return Optional.empty();
        }
    }

    public void delete( String id ) {
        remove( id ).ifPresent( m -> fireDeleted( m.object ) );
    }

    @Override
    public long size() {
        return data.size();
    }

    protected void fireUpdated( T object ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.updated( object );
    }

    protected void fireUpdated( Collection<T> objects ) {
        if( !objects.isEmpty() )
            for( DataListener<T> dataListener : this.dataListeners )
                dataListener.updated( objects );
    }


    protected void fireDeleted( T object ) {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.deleted( object );
    }

    protected void fireDeleted( List<T> objects ) {
        if( !objects.isEmpty() )
            for( DataListener<T> dataListener : this.dataListeners )
                dataListener.deleted( objects );
    }

    public void addDataListener( DataListener<T> dataListener ) {
        this.dataListeners.add( dataListener );
    }

    public void removeDataListener( DataListener<T> dataListener ) {
        this.dataListeners.remove( dataListener );
    }

    @Override
    public List<Metadata<T>> updatedSince( long time ) {
        return Stream.of( data.values() ).filter( m -> m.modified > time ).toList();
    }

    @Override
    public synchronized void close() {
        Scheduled.cancel( rsync );
        Scheduled.cancel( fsync );
        fsync();
        data.clear();
    }

    public interface DataListener<T> {
        default void updated( T object ) {
        }


        default void updated( Collection<T> objects ) {
        }


        default void deleted( T object ) {
        }

        default void deleted( Collection<T> objects ) {
        }

    }
}

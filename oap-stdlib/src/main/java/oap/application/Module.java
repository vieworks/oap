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
package oap.application;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.application.remote.RemoteLocation;
import oap.json.Binder;
import oap.reflect.Coercions;
import oap.util.PrioritySet;
import oap.util.Strings;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@EqualsAndHashCode
@ToString
public class Module {
    public static final String DEFAULT = Strings.DEFAULT;
    public static final ModuleConfiguration CONFIGURATION = new ModuleConfiguration();
    static final Coercions coersions = Coercions.basic().withIdentity();
    @JsonDeserialize( contentUsing = ModuleDependsDeserializer.class )
    public final LinkedHashSet<Depends> dependsOn = new LinkedHashSet<>();
    @JsonAlias( { "service", "services" } )
    public final LinkedHashMap<String, Service> services = new LinkedHashMap<>();
    @JsonAlias( { "profile", "profiles" } )
    public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
    public String name;

    @JsonCreator
    public Module( String name ) {
        this.name = name;
    }

    @EqualsAndHashCode
    @ToString
    public static class Service {
        public final LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        public final Supervision supervision = new Supervision();
        public final LinkedHashSet<String> dependsOn = new LinkedHashSet<>();
        @JsonAlias( { "profile", "profiles" } )
        public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
        public final LinkedHashMap<String, String> listen = new LinkedHashMap<>();
        public final LinkedHashMap<String, Object> link = new LinkedHashMap<>(); // String | Reference
        public String implementation;
        public String name;
        public RemoteLocation remote;
        public boolean enabled = true;

        @JsonIgnore
        public boolean isRemoteService() {
            return remote != null;
        }

    }

    @EqualsAndHashCode
    @ToString
    public static class Supervision {
        public boolean supervise;
        public boolean thread;
        public boolean schedule;
        public List<String> preStartWith = List.of( "preStart", "pre_start" );
        public List<String> startWith = List.of( "start" );
        public List<String> preStopWith = List.of( "preStop", "pre_stop" );
        public List<String> stopWith = List.of( "stop", "close" );
        public long delay; //ms
        public String cron; // http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger
    }

    @ToString
    @EqualsAndHashCode
    public static class Reference {
        public int priority;
        public String name;

        public Reference( int priority, String name ) {
            this.priority = priority;
            this.name = name.substring( name.indexOf( ':' ) + 1 );
        }

        public Reference( String name ) {
            this( PrioritySet.PRIORITY_DEFAULT, name );
        }

        @SuppressWarnings( "unchecked" )
        public static Reference of( Object reference ) {
            if( reference instanceof String )
                return new Reference( ( String ) reference );
            if( reference instanceof Map<?, ?> )
                return Binder.hocon.unmarshal( Reference.class, ( Map<String, Object> ) reference );
            throw new ApplicationException( "could not parse reference " + reference );
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class Depends {
        @JsonAlias( { "name", "service", "module" } )
        public final String name;
        @JsonAlias( { "profile", "profiles" } )
        public final LinkedHashSet<String> profiles = new LinkedHashSet<>();

        public Depends( String name, List<String> profiles ) {
            this.name = name;
            this.profiles.addAll( profiles != null ? profiles : emptyList() );
        }
    }
}

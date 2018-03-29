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

package oap.security.acl;

import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by igor.petrenko on 21.03.2018.
 */
public class RemoteAclSchema implements AclSchema {
    private final AclSchema remoteSchema;

    public RemoteAclSchema( AclSchema remoteSchema ) {
        this.remoteSchema = remoteSchema;
    }

    @Override
    public void validateNewObject( AclObject parent, String newObjectType ) throws AclSecurityException {
        remoteSchema.validateNewObject( parent, newObjectType );
    }

    @Override
    public Optional<AclObject> getObject( String id ) {
        return remoteSchema.getObject( id );
    }

    @Override
    public Stream<AclObject> selectObjects() {
        throw new NotImplementedException( "Stream" );
    }

    @Override
    public List<AclObject> listObjects() {
        return remoteSchema.listObjects();
    }

    @Override
    public Stream<AclObject> selectLocalObjects() {
        throw new NotImplementedException( "Stream" );
    }

    @Override
    public Optional<AclObject> updateLocalObject( String id, Consumer<AclObject> cons ) {
        throw new NotImplementedException( "Stream" );
    }

    @Override
    public Iterable<AclObject> objects() {
        throw new NotImplementedException( "iterable" );
    }

    @Override
    public Iterable<AclObject> localObjects() {
        throw new NotImplementedException( "iterable" );
    }

    @Override
    public void deleteObject( String id ) {
        remoteSchema.deleteObject( id );
    }

    @Override
    public List<String> getPermissions( String objectId ) {
        return remoteSchema.getPermissions( objectId );
    }

    @Override
    public AclSchemaBean addSchema( String owner, AclSchemaBean clientSchema ) {
        return remoteSchema.addSchema( owner, clientSchema );
    }
}
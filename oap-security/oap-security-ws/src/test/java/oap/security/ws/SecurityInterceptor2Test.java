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

package oap.security.ws;

import lombok.val;
import oap.http.Context;
import oap.http.Protocol;
import oap.http.Request;
import oap.http.Session;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.security.acl.AclObject;
import oap.security.acl.AclService;
import oap.ws.WsParam;
import org.apache.http.client.methods.HttpGet;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static oap.ws.Interceptor.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by igor.petrenko on 22.12.2017.
 */
public class SecurityInterceptor2Test {
    private static final Reflection REFLECTION = Reflect.reflect( TestAPI.class );

    private final TokenService2 mockTokenService = mock( TokenService2.class );
    private final AclService mockAclService = mock( AclService.class );

    private final SecurityInterceptor2 securityInterceptor = new SecurityInterceptor2( mockAclService, mockTokenService );

    @Test
    public void testShouldNotCheckMethodWithoutAnnotation() {
        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithoutAnnotation" ) ).get();

        val httpResponse = securityInterceptor.intercept( null, null, methodWithAnnotation, p -> null );

        assertThat( httpResponse ).isEmpty();
    }

    @Test
    public void testShouldVerifyUserIfPresentInSession() {
        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation" ) ).get();

        val userId = "testUser";

        final Session session = new Session();
        session.set( USER_ID, userId );

        when( mockAclService.checkOne( "obj", userId, "parent.read" ) ).thenReturn( true );

        val httpResponse = securityInterceptor.intercept( null,
            session, methodWithAnnotation, p -> "obj" );

        assertThat( httpResponse ).isEmpty();
    }

    @Test
    public void testShouldVerifyAndSetUserInSessionIfAuthorizationHeaderIsPresent() throws UnknownHostException {
        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation" ) ).get();

        val context = new Context( "/", InetAddress.getLocalHost(), Protocol.HTTP.name() );
        val tokenId = UUID.randomUUID().toString();

        val httpRequest = new HttpGet();
        httpRequest.setHeader( "Authorization", tokenId );
        httpRequest.setHeader( "Host", "localhost" );

        val request = new Request( httpRequest, context );

        val userId = "testUser";

        val token = new Token2( tokenId, userId, DateTimeUtils.currentTimeMillis() );

        when( mockTokenService.getToken( tokenId ) ).thenReturn( Optional.of( token ) );

        val session = new Session();
        when( mockAclService.checkOne( "obj", userId, "parent.read" ) ).thenReturn( true );

        val httpResponse = securityInterceptor.intercept( request,
            session, methodWithAnnotation, p -> "obj" );

        assertThat( httpResponse ).isEmpty();
        assertThat( session.get( USER_ID ) ).contains( userId );
    }

    @Test
    public void testAccessDenied() {
        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation" ) ).get();

        val userId = "testUser";

        final Session session = new Session();
        session.set( USER_ID, userId );

        when( mockAclService.checkOne( "obj", userId, "parent.read" ) ).thenReturn( false );

        val httpResponse = securityInterceptor.intercept( null,
            session, methodWithAnnotation, p -> "obj" );

        assertThat( httpResponse ).isPresent();
    }

    @Test
    public void testPostProcessing() {
        when( mockAclService.check( "1", "testUser", "test1.read", "test2.read" ) ).thenReturn( asList( true, false ) );

        final Session session = new Session();
        session.set( USER_ID, "testUser" );

        val methodWithAnnotation = REFLECTION.method( method -> method.name().equals( "methodWithAnnotation" ) ).get();
        assertThat( securityInterceptor.postProcessing( new TestAPI.Res( "1" ), session, methodWithAnnotation ).permissions )
            .containsExactlyInAnyOrder( "test1.read" );
    }

    private static class TestAPI {
        @WsSecurity2( object = "{parent}", permission = "parent.read" )
        @WsSecurityWithPermissions( permission = { "test1.read", "test2.read" } )
        public Res methodWithAnnotation( @WsParam String parent ) {
            return new Res( "1" );
        }

        public void methodWithoutAnnotation() {}

        public static class Res extends AclObject {
            public Res( String id ) {
                super( id, "test" );
            }
        }
    }
}
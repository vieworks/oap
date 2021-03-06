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

package oap.zabbix;

import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;
import oap.json.Binder;
import oap.net.Inet;
import oap.zabbix.logback.ZabbixRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static java.util.Collections.singletonList;

@Slf4j
public class ZabbixImpl implements Zabbix {
    public final String host;
    public final int port;
    public int socketTimeout = 10000;
    public int connectTimeout = 60000;

    public ZabbixImpl( String host, int port ) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Response updateItem( String item, String value ) {
        log.trace( "update item host = {}, item = {}, value= {}", Inet.hostname(), item, value );

        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            socket = new Socket();
            socket.setSoTimeout( socketTimeout );
            socket.connect( new InetSocketAddress( host, port ), connectTimeout );

            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            var data = new Data( Inet.hostname(), item, value );
            var request = new Request( singletonList( data ) );

            ZabbixRequest.writeExternal( request, outputStream );

            var buf = new byte[1024];
            var responseBaos = new ByteArrayOutputStream();

            while( true ) {
                int read = inputStream.read( buf );
                if( read <= 0 ) {
                    break;
                }
                responseBaos.write( buf, 0, read );
            }

            var bResponse = responseBaos.toByteArray();

            if( bResponse.length < 13 ) {
                log.trace( "response.length < 13" );
                return new Response( "[]", "" );
            } else {
                String jsonString = new String( bResponse, 13, bResponse.length - 13, StandardCharsets.UTF_8 );
                log.trace( "response = {}", jsonString );
                return Binder.json.unmarshal( Response.class, jsonString );
            }
        } catch( IOException e ) {
            log.error( e.getMessage() );
            log.debug( e.getMessage(), e );

            return new Response( e.getMessage(), "" );
        } finally {
            Closeables.close( inputStream );
            Closeables.close( outputStream );
            Closeables.close( socket );
        }
    }
}

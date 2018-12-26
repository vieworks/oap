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

package oap.ws.validate.testng;

import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;
import oap.ws.validate.WsValidateJson;
import org.testng.annotations.Test;

import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;
import static oap.ws.validate.testng.ValidationErrorsAssertion.validating;
import static org.assertj.core.api.Assertions.assertThat;

public class ValidationErrorsAssertionTest {
    @Test
    public void validatedCall() {
        assertThat(
            validating( I.class )
                .isError( 404, "not found" )
                .forInstance( new C() )
                .m( "a" ) )
            .isNull();
        assertThat(
            validating( I.class )
                .isFailed()
                .hasCode( 404 )
                .containsErrors( "not found" )
                .forInstance( new C() )
                .m( "b" ) )
            .isNull();
        assertThat( validating( I.class )
            .isNotFailed()
            .forInstance( new C() )
            .m( "c" ) )
            .isEqualTo( "c" );
    }

    @Test
    public void validateSchemaPreUnmarshalValidation() {
        validating( I.class )
            .isFailed()
            .hasCode( 400 )
            .containsErrors( "/b: required property is missing", "additional properties are not permitted [a]" )
            .forInstance( new C() )
            .b( new B() );
    }

}

@SuppressWarnings( "unused" )
class B {
    int a;
}

interface I {
    String m( String a );

    B b( B b );
}

@SuppressWarnings( "unused" )
class C implements I {
    @WsValidate( "validateM" )
    @Override
    public String m( @WsValidate( "validateP" ) String a ) {
        return a;
    }

    @Override
    public B b( @WsValidateJson( schema = "/oap/ws/validate/testng/ValidationErrorsAssertionTest/schema.conf" ) B b ) {
        return b;
    }

    public ValidationErrors validateM( String a ) {
        return a.equals( "a" ) ? error( 404, "not found" )
            : empty();
    }

    public ValidationErrors validateP( String a ) {
        return a.equals( "b" ) ? error( 404, "not found" )
            : empty();
    }
}

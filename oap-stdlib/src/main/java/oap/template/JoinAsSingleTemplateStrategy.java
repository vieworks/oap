package oap.template;

public class JoinAsSingleTemplateStrategy implements TemplateStrategy<JavaCTemplate.Line> {

    @Override
    public void mapFirstJoin( StringBuilder c, JavaCTemplate.Line line ) {
        c.append( "\njb = new StringBuilder();\n" );
    }

    @Override
    public void mapLastJoin( StringBuilder c, JavaCTemplate.Line line ) {
        c.append( "\nacc.accept( " );
        function( c, line.function, () -> escape( c, () -> c.append( " jb.toString()" ) ) );
        c.append( " );" );
    }

    @Override
    public void mapCollection( StringBuilder c, FieldInfo cc, JavaCTemplate.Line line, String field ) {
        c.append( "{acc.accept( '[' + " );
        function( c, line.function, () -> escape( c, () -> c.append( " Strings.join( " ).append( field ).append( " )" ) ) );
        c.append( " + ']' );}" );
    }

    @Override
    public void mapObject( StringBuilder c, FieldInfo cc, JavaCTemplate.Line line, String field, boolean isJoin ) {
        if( isJoin ) {
            c.append( "jb.append( " );
        } else {
            c.append( "acc.accept( " );
        }
        function( c, line.function, () -> escape( c, () -> c.append( " String.valueOf( " ).append( field ).append( " )" ) ) );
        c.append( " );" );
    }

    @Override
    public void mapPrimitive( StringBuilder c, FieldInfo cc, JavaCTemplate.Line line, String field, boolean isJoin ) {
        if( isJoin ) {
            c.append( "jb.append( " );
        } else {
            c.append( "acc.accept( " );
        }
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
    }

    @Override
    public void mapInterJoin( StringBuilder c, FieldInfo cc, JavaCTemplate.Line line, String field ) {
        c.append( "jb.append( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );\n" );
    }

    @Override
    public void mapString( StringBuilder c, FieldInfo cc, JavaCTemplate.Line line, String field, boolean isJoin ) {
        if( isJoin ) {
            c.append( "jb.append( " );
        } else {
            c.append( "acc.accept( " );
        }
        function( c, line.function, () -> escape( c, () -> c.append( field ) ) );
        c.append( " );" );
    }

    @Override
    public void mapEnum( StringBuilder c, FieldInfo cc, JavaCTemplate.Line line, String field, boolean isJoin ) {
        if( isJoin ) {
            c.append( "jb.append( " );
        } else {
            c.append( "acc.accept( " );
        }
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
    }
}

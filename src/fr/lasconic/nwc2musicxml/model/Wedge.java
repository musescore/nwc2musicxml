package fr.lasconic.nwc2musicxml.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Wedge implements IElement{

	/**
	 * There are 3 types of Wedges: Crescendo, Diminuendo, and Stop 
	 */
	public String type;
	
	public Wedge( String _type ) {
		type = _type;
	}
	
	
	public static void findWedges( String[] options, Measure measure, int voiceId ) {
		//Whether a wedge (cresc/dimin) was found in the options
		boolean noWedge = true;

		for( String s: options ) {			
			if( s.contains( "Diminuendo" ) ) {
				if( currentWedge != null && currentWedge.equals( "Crescendo" ) ) {
					measure.addElement( new Wedge( "Stop" ), voiceId );
					currentWedge = null;
//					System.out.println( "Stop!" );
				}	
				if( currentWedge == null ) {
					measure.addElement( new Wedge( "Diminuendo" ), voiceId );
					currentWedge = "Diminuendo";
//					System.out.println( "Dim!" );
				}
				noWedge = false;
			} else if( s.contains( "Crescendo" ) ) {
				if( currentWedge != null && currentWedge.equals( "Diminuendo" ) ) {
					measure.addElement( new Wedge( "Stop" ), voiceId );
					currentWedge = null;
//					System.out.println( "Stop!" );
				}
				if( currentWedge == null ) {
					measure.addElement( new Wedge( "Crescendo" ), voiceId );
					currentWedge = "Crescendo";
//					System.out.println( "Cresc!" );
				}
				noWedge = false;
			} 
		}
		if( noWedge && currentWedge != null ) {
			measure.addElement( new Wedge( "Stop" ), voiceId );
			currentWedge = null;
//			System.out.println( "Stop!" );
		}
	}
	
	/**
	 * This is the crescendo or diminuendo currently in effect. If neither, is null.
	 */
	public static String currentWedge = null;

/*	<direction placement="above">
	<direction-type>
		<wedge type="diminuendo" spread="11"/>
	</direction-type>
</direction>


<direction>
	<direction-type>
		<wedge type="stop" spread="0"/>
	</direction-type>
</direction>*/


	/**
	 * Converts this Wedge into an XML element to put in the DOM.
	 * @return an Element form of this Wedge
	 */
	public Element toElement( Document doc ) {
		Element direction = doc.createElement( "direction" );
		direction.setAttribute( "placement", "below" );
		
		Element dType = doc.createElement( "direction-type" );
		
		Element wedge = doc.createElement( "wedge" );
		wedge.setAttribute( "type", type.toLowerCase() );
		wedge.setAttribute( "spread", "" + ( type.equals( "Stop" ) ? 0 : 11 ) );
		
		dType.appendChild( wedge );
		direction.appendChild( dType );
		
		return direction;
	}


}

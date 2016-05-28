[![Build Status](https://travis-ci.org/lasconic/nwc2musicxml.svg?branch=master)](https://travis-ci.org/lasconic/nwc2musicxml)

The primary goal of this project is to provide a library to convert NWC scores to MusicXML. Then, Noteworthy users will be able to share their scores with others scorewriters, especially free ones such as [MuseScore](https://musescore.org). For the time being, the library support only NWCtxt and you need a copy of NoteWhorthy composer to save a NWC file to NWCtxt.

The converter is also available online as a webservice on Google App Engine at https://nwc2musicxml.appspot.com/

If you find the software useful you can [donate](https://paypal.me/lasconic).

Features
==

* Multiple staves & voices, invisible staff not exported
* Notes, rests, accidentals, dots, time signature
* Key signature (not the customized ones)
* Clefs included octave shift
* Tie & slurs, triplets
* Line breaks
* Lyrics
* Metadata: Title, author, lyricist, copyright
* Staccato, tenuto & accent, noteheads
* Dynamics (mf, p ...) & custom texts
* Midi info for staff
* Beaming


Not yet supported
---
* Chord symbols, grace notes
* Flow control : Coda, Segno, ...
* Dynamic variation
* Layout information


Thanks to [Noteworthy Software](http://www.noteworthysoftware.com/) for giving me a free licence for testing purpose.

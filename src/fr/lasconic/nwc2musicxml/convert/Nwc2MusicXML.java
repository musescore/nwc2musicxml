package fr.lasconic.nwc2musicxml.convert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.InflaterInputStream;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.lasconic.nwc2musicxml.model.Clef;
import fr.lasconic.nwc2musicxml.model.Dynamics;
import fr.lasconic.nwc2musicxml.model.Ending;
import fr.lasconic.nwc2musicxml.model.EndingSet;
import fr.lasconic.nwc2musicxml.model.Flow;
import fr.lasconic.nwc2musicxml.model.Forward;
import fr.lasconic.nwc2musicxml.model.IElement;
import fr.lasconic.nwc2musicxml.model.Key;
import fr.lasconic.nwc2musicxml.model.Lyric;
import fr.lasconic.nwc2musicxml.model.Lyrics;
import fr.lasconic.nwc2musicxml.model.Measure;
import fr.lasconic.nwc2musicxml.model.Metadata;
import fr.lasconic.nwc2musicxml.model.Note;
import fr.lasconic.nwc2musicxml.model.Part;
import fr.lasconic.nwc2musicxml.model.Score;
import fr.lasconic.nwc2musicxml.model.Staff;
import fr.lasconic.nwc2musicxml.model.Tempo;
import fr.lasconic.nwc2musicxml.model.TempoVariance;
import fr.lasconic.nwc2musicxml.model.Text;
import fr.lasconic.nwc2musicxml.model.TimeSig;
import fr.lasconic.nwc2musicxml.model.Wedge;

public class Nwc2MusicXML implements IConstants {

	private double nwctxtVersion;

	private Score score;
	private Staff staff;
	private Part p;
	private int currentStaffId;
	private Measure measure;
	private EndingSet endingSet = new EndingSet();
	private int measureId; // used for initial scan for endings

	public Nwc2MusicXML() {
		// when nwctxtVersion < 1, the nwctxt header has not yet been detected
		nwctxtVersion = 0.0;
		currentStaffId = 0;
	}

	public ConversionResult convert(InputStream in) {

		String tuneAsString = null;
		try {
			int len;
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();

			byte[] refNWZ = { '[', 'N', 'W', 'Z', ']', 0 };
			byte[] hdrNWZ = new byte[refNWZ.length];
			boolean success;

			success = (in.read(hdrNWZ) == refNWZ.length);

			if (success) {
				if (Arrays.equals(hdrNWZ, refNWZ)) {
					in = new InflaterInputStream(in);
				} else {
					outStream.write(hdrNWZ, 0, refNWZ.length);
				}
			} else {
				return new ConversionResult();
			}

			byte[] buffer = new byte[8192];
			while ((len = in.read(buffer, 0, buffer.length)) != -1) {
				outStream.write(buffer, 0, len);
			}

			tuneAsString = outStream.toString("UTF-8");
			System.out.println(tuneAsString);

		} catch (IOException e) {
			e.printStackTrace();
		}
		if (tuneAsString != null)
			return convert(tuneAsString);
		else
			return new ConversionResult();
	}

	public ConversionResult convert(String in) {
		int res = ConversionResult.CONTINUE;
		score = new Score();
		ConversionResult result = new ConversionResult();
		try {
			// initial scan for Endings
			BufferedReader input = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(
					in.getBytes("UTF-8")), "UTF-8"));
			try {
				String line = null; // not declared within while loop

				while ((line = input.readLine()) != null) {
					res = processEndings(line);
					if (res != ConversionResult.CONTINUE)
						break;
				}
			} finally {
				input.close();
				currentStaffId = 0;
				endingSet.prepareEndings();
			}

			// check the NWCTXT file has valid format (flow control is
			// consistent across staves
			if (!endingSet.compareStaves()) {
				System.err
						.println("NWCTXT file has inconsistent flow control across staves - the file is invalid and may not import correctly");
			}
			// main file processes to convert to MusicXML
			input = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(in.getBytes("UTF-8")), "UTF-8"));
			try {
				String line = null; // not declared within while loop

				while ((line = input.readLine()) != null) {
					res = processLine(line);
					if (res != ConversionResult.CONTINUE)
						break;
				}
			} finally {
				input.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		result.setErrorCode(res);
		if (res == ConversionResult.END_OF_FILE) {
			String title = "test";
			if (score.metadata != null && score.metadata.title != null) {
				title = score.metadata.title;
			}
			result.setTitle(title);
		}
		return result;
	}

	public int write(OutputStream out) {
		Document doc = createMusicXmlDOM();
		try {
			writeAsMusicXML(doc, new BufferedWriter(new OutputStreamWriter(out, "UTF-8")));

			return 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	private boolean init() {
		boolean result = false;
		if (staff == null) {
			p = new Part();
			score.addPart(p);
			currentStaffId = 0;
			staff = new Staff();
			p.addStaff(staff);
			currentStaffId++;
			measure = new Measure();

			staff.addMeasure(measure);
			result = true;
		}
		return result;
	}

	private int processEmptyErrorLines(String line) {
		if (line.startsWith("#") || line.trim().length() == 0) {
			return ConversionResult.CONTINUE;
		}

		if ((nwctxtVersion < 1) && line.startsWith("[NoteWorthy ArtWare]") && !line.contains("!NoteWorthyComposer(")) {
			return ConversionResult.ERROR_OLD_VERSION;
		}

		if ((nwctxtVersion < 1) && line.contains("!NoteWorthyComposer(")) {
			Pattern versionPattern = Pattern.compile("!NoteWorthyComposer\\(([0-9]*\\.?[0-9]*)");
			Matcher versionMatcher = versionPattern.matcher(line);

			if (versionMatcher.find()) {
				nwctxtVersion = Double.parseDouble(versionMatcher.group(1));
			} else {
				// set as default of version 99...indicates the header has been
				// found
				// but the version is unknown
				nwctxtVersion = 9999.9999;
			}

			return ConversionResult.CONTINUE;
		}

		return ConversionResult.CONTINUE;
	}

	// Need to read and store the endings ahead of normal processing, because
	// NWC doesn't
	// define the end of a volta - so we create voltas that start where NWC
	// starts them
	// and either end when the next volta in the set starts, or discontinue at
	// the end of
	// the bar the volta is placed if it is the last volta in a set
	// Also review here whether voltas, segnos and double bar lines are
	// different between
	// staves - and warn if so
	private int processEndings(String line) {
		int result = processEmptyErrorLines(line);
		if (result != ConversionResult.CONTINUE) {
			return result;
		}
		String[] sArray2;
		String sA;

		if (line.startsWith("!NoteWorthyComposer-End") || line.startsWith("!NoteWorthyComposerClip-End")) {
			// process the captured endings and work out which are
			// stop/discontinue
			endingSet.addLastBar(currentStaffId, measureId);
			return ConversionResult.END_OF_FILE;
		}

		if (line.startsWith("|")) {
			String[] sArray = line.split("\\|");
			if (sArray.length > 0) {
				String type = sArray[1];
				// System.err.println(type);
				if (type.compareTo("AddStaff") == 0) { // Add Staff
					if (currentStaffId > 0) {
						endingSet.addLastBar(currentStaffId, measureId);
					}
					currentStaffId++;
					measureId = 0;
				} else if (type.compareTo("Bar") == 0) {
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Style")) {
							sArray2 = sA.split(":");
							endingSet.addDoubleBarline(currentStaffId, measureId, sArray2[1]);
						}
					}
					measureId++;
				} else if (type.compareTo("Ending") == 0) {
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Endings")) {
							sArray2 = sA.split(":");
							endingSet.addEnding(currentStaffId, measureId, sArray2[1]);
						}
					}
				} else if (type.compareTo("Flow") == 0) {
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Style")) {
							sArray2 = sA.split(":");
							endingSet.addFlow(currentStaffId, measureId, sArray2[1]);
						}
					}

				}
			}
		}
		return ConversionResult.CONTINUE;
	}

	private int processLine(String line) {
		int result = processEmptyErrorLines(line);
		if (result != ConversionResult.CONTINUE) {
			return result;
		}

		String[] sArray2;
		String sA;
		String sB;
		boolean chordHasTwoVoices;
		int voiceId = (currentStaffId - 1) * 4 + 1;

		if (line.startsWith("!NoteWorthyComposer-End") || line.startsWith("!NoteWorthyComposerClip-End")) {
			// reset wedge (cresc/dimin)
			if (Wedge.currentWedge != null) {
				measure.addElement(new Wedge("Stop"), voiceId);
			}
			// Check for unclosed ending at last bar
			// until NWC supports multi-bar endings, we always use EndingSet to
			// work out when endings close
			String endingToClose = endingSet.getEnding(currentStaffId, measureId);
			if (endingToClose.compareTo("") != 0) {
				Ending ending = new Ending();
				ending.number = endingSet.getEndingNumber(currentStaffId, measureId);
				ending.type = endingSet.getEndingType(currentStaffId, measureId);
				// deliberately on measure (last measure) not newMeasure
				measure.addElement(ending, voiceId);
			}

			Wedge.currentWedge = null;
			// remove last measure if empty
			if (measure != null && measure.isEmpty()) {
				staff.measures.remove(measure);
			}
			return ConversionResult.END_OF_FILE;
		}

		if (line.startsWith("|")) {
			if (line.contains("Pos2") && line.contains("Chord") && line.contains("Stem=")) {
				String firstStem = line.replaceAll(".*Stem=", "");
				chordHasTwoVoices = true;
				firstStem = line.replaceAll("|.*", "");
				line += "|Optz=";
				if (firstStem.compareTo("Up") == 0) {
					line += "Down";
				} else if (firstStem.compareTo("Down") == 0) {
					line += "Up";
				}
				// this is a dummy tag to allow an opportunity to set stem when
				// it isn't set in input
			} else {
				chordHasTwoVoices = false;
			}
			String[] sArray = line.split("\\|");
			if (sArray.length > 0) {
				String type = sArray[1];
				// System.err.println(type);
				if (type.compareTo("AddStaff") == 0) { // Add Staff
					// reset wedge (cresc/dimin)
					if (Wedge.currentWedge != null) {
						measure.addElement(new Wedge("Stop"), voiceId);
					}
					Wedge.currentWedge = null;
					// remove last measure if empty
					if (measure != null && measure.isEmpty()) {
						staff.measures.remove(measure);
					}
					staff = new Staff();
					currentStaffId++;

					// track measure number for labeling endings
					measureId = 0;

					measure = new Measure();
					staff.addMeasure(measure);
					String partName = null;
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Label")) {
							sArray2 = sA.split(":");
							if (sArray2[1].length() > 2) {
								String label = sArray2[1].substring(1, sArray2[1].length() - 1);
								partName = label;
							}

						} else if (sA.contains("Name")) {
							sArray2 = sA.split(":");
							staff.name = sArray2[1];
						} else if (sA.contains("Group")) {
							sArray2 = sA.split(":");
							staff.group = sArray2[1];
						}
					}
					// create a new part only if needed
					if (p == null || !p.containsStaffForGroup(staff.group)) {
						p = new Part();
						if (partName != null)
						      p.name = partName;
						currentStaffId = 1;
						score.addPart(p);
					}
					p.addStaff(staff);
				} else if (type.compareTo("StaffProperties") == 0) { // StaffProperties
					init();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.startsWith("Visible")) {
							sArray2 = sA.split(":");
							if (sArray2[1].contains("N")) {
								staff.visible = false;
							}
						} else if (sA.startsWith("Style")) {
							sArray2 = sA.split(":");
							if (sArray2[1].contains("Lower Grand Staff")) {
								Part part = score.parts.get(score.parts.size() - 2);
								if (part != null) {
									score.parts.remove(p);
									p = part;
									p.addStaff(staff);
								}
							}
						} else if (sA.startsWith("Channel")) {
							sArray2 = sA.split(":");
							p.channel = sArray2[1];
						} else if (sA.startsWith("EndingBar")) {
							sArray2 = sA.split(":");
							if ("Double".compareTo(sArray2[1]) == 0) {
								p.barLineStyle = "light-light";
							} else if ("MasterRepeatClose".compareTo(sArray2[1]) == 0) {
								p.barLineStyle = "light-heavy";
								p.endRepeat = true;
							} else if ("Section Close".compareTo(sArray2[1]) == 0) {
								p.barLineStyle = "light-heavy";
							} else if ("Single".compareTo(sArray2[1]) == 0) {
								p.barLineStyle = "regular";
							}
						}
					}
				} else if (type.compareTo("StaffInstrument") == 0) { // StaffProperties
					init();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.startsWith("Patch")) {
							sArray2 = sA.split(":");
							try {
								p.program = Integer.parseInt(sArray2[1]);
							} catch (NumberFormatException e) {
								p.program = -1;
							}
						} else if (sA.startsWith("Trans")) {
							sArray2 = sA.split(":");
							try {
								p.trans = Integer.parseInt(sArray2[1]);
							} catch (NumberFormatException e) {
								p.trans = 0;
							}
						}
					}
				} else if (type.compareTo("SongInfo") == 0) { // SongInfo
					Metadata metadata = new Metadata();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.startsWith("Title")) {
							sArray2 = sA.split(":");
							if (sArray2[1].length() > 2)
								metadata.title = sArray2[1].substring(1, sArray2[1].length() - 1);
						} else if (sA.startsWith("Author")) {
							sArray2 = sA.split(":");
							if (sArray2[1].length() > 2)
								metadata.author = sArray2[1].substring(1, sArray2[1].length() - 1);
						} else if (sA.startsWith("Lyricist")) {
							sArray2 = sA.split(":");
							if (sArray2[1].length() > 2)
								metadata.lyricist = sArray2[1].substring(1, sArray2[1].length() - 1);
						} else if (sA.startsWith("Copyright1")) {
							sArray2 = sA.split(":");
							if (sArray2[1].length() > 2)
								metadata.copyright = sArray2[1].substring(1, sArray2[1].length() - 1);
						} else if (sA.startsWith("Copyright2")) {
							sArray2 = sA.split(":");
							if (sArray2[1].length() > 2) {
								if (!metadata.copyright.isEmpty()) {
									metadata.copyright += "\n";
								}
								metadata.copyright += sArray2[1].substring(1, sArray2[1].length() - 1);
							}
						}
					}
					score.metadata = metadata;
				} else if (type.compareTo("Clef") == 0) { // Clef
					init();
					Clef clef = new Clef();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Type")) {
							sArray2 = sA.split(":");
							clef.type = sArray2[1];
						} else if (sA.contains("OctaveShift")) {
							sArray2 = sA.split(":");
							if (sArray2[1].contains("Down")) {
								clef.octaveShift = -1;
							} else if (sArray2[1].contains("Up")) {
								clef.octaveShift = 1;
							}
						}
					}
					measure.addElement(clef, voiceId);
				} else if (type.matches("Lyric[1-9]")) { // Lyrics
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Text")) {
							sArray2 = sA.split(":");
							int index = sA.indexOf(':');
							if (index > 3) {
								String l = sA.substring(index + 1);
								Lyrics lyrics = new Lyrics();
								lyrics.parse(l.substring(1, l.length() - 1));
								staff.lyricsLine.add(lyrics);
							}
						}
					}

				} else if (type.compareTo("Note") == 0) { // Note
					init();
					measure.notesCount++;
					voiceId = (currentStaffId - 1) * 4 + 1;
					Note note = new Note();
					note.firstInChord = true;

					String[] optsArr = {};
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Pos")) {
							sArray2 = sA.split(":");
							note.setPos(sArray2[1]);
						} else if (sA.contains("Dur")) {
							sArray2 = sA.split(":");
							note.dur = sArray2[1];
						} else if (sA.contains("Opts")) {
							sArray2 = sA.split(":");
							note.setOpts(sArray2[1]);
							optsArr = sArray2[1].split(",");
							// find cresc/dimin
						}
					}
					// if measure has voices already and stem Down, put it in
					// voice 2
					int voiceOffset = 0;
					if (measure.hasVoices()) {
						for (String s : optsArr) {
							if (s.startsWith("Stem")) {
								String[] sArray3 = s.split("=");
								if (sArray3.length > 1 && sArray3[1].compareTo("Down") == 0)
									voiceOffset = 1;
							}
						}
					}
					Wedge.findWedges(optsArr, measure, voiceId + voiceOffset);
					measure.addElement(note, voiceId + voiceOffset);
				} else if (type.compareTo("Chord") == 0 || type.compareTo("RestChord") == 0) { // Chord
					init();
					measure.notesCount++;
					voiceId = (currentStaffId - 1) * 4 + 1;
					String dur = "";
					String dur2 = "";
					ArrayList<Note> notes = new ArrayList<Note>();

					String[] optsArr = {};
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.startsWith("Opts")) {
							sArray2 = sA.split(":");
							if (sArray2.length > 1)
								optsArr = sArray2[1].split(",");
						}
					}
					// find cresc/dimin
					Wedge.findWedges(optsArr, measure, voiceId);
					boolean pos1 = false, pos2 = false;
					int voiceAdj = 0;
					String stemActive = "";
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Pos2")) {
							// means there are 2 voices in the chord
							// change required is to look at stem up/down - use
							// up for voice 1 and down for voice 2
							// easy for Pos2 but need to look ahead for Pos in
							// case there is a Pos2
							pos2 = true;
							sArray2 = sA.split(":");
							String[] sArray3 = sArray2[1].split(",");
							boolean chord = false;
							if (sArray3.length > 1) {
								chord = true;
							}
							for (int j = 0; j < sArray3.length; j++) {
								Note note = new Note();
								note.dur = dur2;
								note.setPos(sArray3[j]);
								if (stemActive.compareTo("Up") == 0) {
									note.stem = "down";
								} else if (stemActive.compareTo("Down") == 0) {
									note.stem = "up";
								}
								note.chord = chord;
								if (j == 0) {
									note.firstInChord = true;
									if (measure.measureOffset > 0 && ((1 - voiceAdj) == 1)) {
										measure.addElement(new Forward(measure.measureOffset), voiceId + 1 - voiceAdj);
									}
								}
								measure.addElement(note, voiceId + 1 - voiceAdj);
								measure.measureOffset = -1 * note.getDuration();
							}
						} else if (sA.contains("Dur2")) {
							sArray2 = sA.split(":");
							dur2 = sArray2[1];
						} else if (sA.contains("Pos")) {
							pos1 = true;
							// Stem if present is always on Pos (and Pos2 is the
							// opposite stem)
							for (int j = 0; j < optsArr.length; j++) {
								sB = optsArr[j];
								if (sB.startsWith("Stem")) {
									String[] sArray4 = sB.split("=");
									if (chordHasTwoVoices) {
										if (sArray4.length > 1 && sArray4[1].compareTo("Up") == 0) {
											// voiceAdj is used to flip the
											// voice (which NWC notates as a
											// combination of
											// Stem and Pos/Pos2
											voiceAdj = 0;
										} else {
											voiceAdj = 1;
										}
									} else {
										// the voiceAdj value can only be used
										// when there are two voices in the
										// chord
										// if not - set to zero
										voiceAdj = 0;
									}
									stemActive = sArray4[1];
								}
							}
							// voiceAdj is used to adjust the voice used to add
							// notes in a chord for both Pos and Pos2

							sArray2 = sA.split(":");
							if (sArray2[0].compareTo("Pos") == 0) {
								String[] sArray3 = sArray2[1].split(",");
								boolean chord = false;
								if (sArray3.length > 1) {
									chord = true;
								}
								for (int j = 0; j < sArray3.length; j++) {
									Note note = new Note();
									note.dur = dur;
									note.setPos(sArray3[j]);
									note.chord = chord;
									if (j == 0) {
										note.firstInChord = true;
										if (measure.measureOffset > 0 && voiceAdj == 1)
											measure.addElement(new Forward(measure.measureOffset), voiceId + voiceAdj);
									}
									// Adjust the voice based on Stem
									measure.addElement(note, voiceId + voiceAdj);
									if (line.contains("Dur2"))
										measure.measureOffset -= note.getDuration();
									notes.add(note);
								}
							}
						} else if (sA.contains("Dur")) {
							sArray2 = sA.split(":");
							if (sArray2[0].compareTo("Dur") == 0) {
								dur = sArray2[1];
							}
						} else if (sA.contains("Opts")) {
							sArray2 = sA.split(":");
							for (Note note : notes) {
								note.setOpts(sArray2[1]);
							}
						} else if (sA.contains("Optz")) {
							sArray2 = sA.split("=");
							String otherVoiceStem = null;
							if (sArray2.length == 2) {
								otherVoiceStem = sA.split("=")[1];
							}
							if (chordHasTwoVoices && otherVoiceStem != null) {
								for (Note note : notes) {
									note.setOpts(otherVoiceStem);
								}
							}
						}

						// need to do the above with the opposite Stem on the
						// other set of Notes
					}
					if (type.compareTo("RestChord") == 0) {
						if (!pos1) {
							Note note = new Note();
							note.rest = true;
							note.firstInChord = true;
							note.dur = dur;
							measure.addElement(note, voiceId);
							if (line.contains("Dur2"))
								measure.measureOffset -= note.getDuration();
						}
						if (!pos2) {
							Note note = new Note();
							note.rest = true;
							note.firstInChord = true;
							note.dur = dur2;
							if (measure.measureOffset > 0) {
								measure.addElement(new Forward(measure.measureOffset), voiceId + 1);
							}
							measure.addElement(note, voiceId + 1);
							measure.measureOffset = -1 * note.getDuration();
						}
					}
				} else if (type.compareTo("Rest") == 0) { // Rest
					init();
					measure.notesCount++;
					voiceId = (currentStaffId - 1) * 4 + 1;
					Note note = new Note();
					note.rest = true;
					note.firstInChord = true;

					String[] optsArr = {};
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Dur")) {
							sArray2 = sA.split(":");
							note.dur = sArray2[1];
						} else if (sA.contains("Opts")) {
							sArray2 = sA.split(":");
							optsArr = sArray2;
						}
					}
					if ("Whole".compareTo(note.dur) == 0) {
						measure.wholeRest = true;
					}
					// find cresc/dimin
					Wedge.findWedges(optsArr, measure, voiceId);
					measure.addElement(note, voiceId);
				} else if (type.compareTo("Bar") == 0) {
					Measure newMeasure;

					// track measure numbers to label endings
					measureId++;

					if (!init()) {
						if (staff.measures.size() == 1) {
							// maybe it's first barline
							Measure m = staff.measures.get(0);
							if (m.notesCount > 0) {
								// we already have some notes, let's create a
								// new measure
								newMeasure = new Measure();
								staff.addMeasure(newMeasure);
							} else {
								// could be a left barline, let's use the same
								// measure
								newMeasure = measure;
							}
						} else {
							newMeasure = new Measure();
							staff.addMeasure(newMeasure);
						}
					} else {
						newMeasure = measure;
					}

					// check whether an ending needs to be closed here
					String endingToClose = endingSet.getEnding(currentStaffId, measureId);
					if (endingToClose.compareTo("") != 0) {
						Ending ending = new Ending();
						ending.number = endingSet.getEndingNumber(currentStaffId, measureId);
						ending.type = endingSet.getEndingType(currentStaffId, measureId);
						// deliberately on measure (last measure) not newMeasure
						measure.addElement(ending, voiceId);
					}
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.startsWith("SysBreak")) {
							sArray2 = sA.split(":");
							if ("Y".compareTo(sArray2[1]) == 0) {
								measure.lineBreak = true;
							}
						} else if (sA.startsWith("Style")) {
							sArray2 = sA.split(":");
							if ("Double".compareTo(sArray2[1]) == 0) {
								measure.rightBarType = "light-light";
							} else if ("MasterRepeatOpen".compareTo(sArray2[1]) == 0) {
								newMeasure.leftBarType = "heavy-light";
								newMeasure.leftRepeat = true;
							} else if ("MasterRepeatClose".compareTo(sArray2[1]) == 0) {
								measure.rightBarType = "light-heavy";
								measure.rightRepeat = true;
							} else if ("LocalRepeatOpen".compareTo(sArray2[1]) == 0) {
								// newMeasure.leftBarType = "light-light";
								newMeasure.leftBarType = "heavy-light";
								// neither Sibelius or MuseScore recognise a
								// light-light repeat bar
								// only heavy-light - and with light-light the
								// repeat times parameter is ignored
								// so change to heavy-light
								newMeasure.leftRepeat = true;
							} else if ("LocalRepeatClose".compareTo(sArray2[1]) == 0) {
								// measure.rightBarType = "light-light";
								measure.rightBarType = "light-heavy";
								// possible adjustment - neither Sibelius or
								// MuseScore recognise a light-light repeat bar
								// only heavy-light - and with light-light the
								// repeat times parameter is ignored
								// so change to heavy-light
								measure.rightRepeat = true;
							} else if ("SectionOpen".compareTo(sArray2[1]) == 0) {
								newMeasure.leftBarType = "heavy-light";
							} else if ("SectionClose".compareTo(sArray2[1]) == 0) {
								measure.rightBarType = "light-heavy";
							}
						} else if (sA.startsWith("Repeat")) {
							// NWC only supports this for LocalRepeatClose
							measure.repeatTimes = sA.split(":")[1];
						}
					}
					measure = newMeasure;
				} else if (type.compareTo("Key") == 0) {
					init();
					Key key = new Key();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.startsWith("Signature")) {
							sArray2 = sA.split(":");
							key.parse(sArray2[1]);
						}
					}
					measure.addElement(key, voiceId);
				} else if (type.compareTo("TimeSig") == 0) {
					init();
					TimeSig timeSig = new TimeSig();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.startsWith("Signature")) {
							sArray2 = sA.split(":");
							timeSig.parse(sArray2[1]);
						}
					}
					measure.addElement(timeSig, voiceId);
				} else if (type.compareTo("Tempo") == 0) {
					init();
					Tempo tempo = new Tempo();
					tempo.parse(sArray);
					measure.addElement(tempo, voiceId);
				} else if (type.compareTo("Text") == 0) {
					init();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.startsWith("Text")) {
							sArray2 = sA.split(":");
							if (sArray2[1].length() > 2) {
								Text text = new Text();
								text.text = sArray2[1].substring(1, sArray2[1].length() - 1);
								measure.addElement(text, voiceId);
							}
						}
					}
				} else if (type.compareTo("Dynamic") == 0) {
					init();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Style")) {
							sArray2 = sA.split(":");
							Dynamics text = new Dynamics();
							text.text = sArray2[1];
							measure.addElement(text, voiceId);

						}
					}
				} else if (type.compareTo("Flow") == 0) {
					init();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Style")) {
							sArray2 = sA.split(":");
							Flow flow = new Flow();
							flow.flow = sArray2[1];
							measure.addElement(flow, voiceId);
						}
					}
				} else if (type.compareTo("Ending") == 0) {
					init();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Endings")) {
							sArray2 = sA.split(":");
							Ending ending = new Ending();
							ending.number = sArray2[1];
							ending.type = "start"; // NWC doesn't track
													// multi-bar
							measure.addElement(ending, voiceId);
						}
					}
				} else if (type.compareTo("TempoVariance") == 0) {
					init();
					for (int i = 2; i < sArray.length; i++) {
						sA = sArray[i];
						if (sA.contains("Style")) {
							sArray2 = sA.split(":");
							if ((sArray2[1].compareTo("Fermata") == 0) || (sArray2[1].compareTo("Breath Mark") == 0)) {
								TempoVariance tempoVariance = new TempoVariance();
								tempoVariance.type = sArray2[1];
								measure.addElement(tempoVariance, voiceId);
							} else {
								// these are Tempo Variance directions
								TempoVariance tempoVariance = new TempoVariance();
								tempoVariance.type = sArray2[1];
								measure.addElement(tempoVariance, voiceId);
							}
						}
					}
				}

			}
			try {
				// System.err.println( staff.measures.size() + ": " );
				for (IElement e : measure.voices.get(voiceId)) {
					if (e instanceof Wedge) {
						// System.err.println(((Wedge) e).type);
					} else {
						// System.err.println( "something" );
					}
				}
				// System.err.println();
			} catch (NullPointerException e) {

			}
		}
		return ConversionResult.CONTINUE;
	}

	/**
	 * Writes the specified Node to the given writer.
	 * 
	 * @param node
	 *            A DOM node.
	 * @param writer
	 *            A stream writer.
	 * @throws IOException
	 *             Thrown if the file cannot be created.
	 */
	public void writeAsMusicXML(Document doc, BufferedWriter writer) throws IOException {

		try {
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			// trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			trans.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Recordare//DTD MusicXML 2.0 Partwise//EN");
			trans.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.musicxml.org/dtds/partwise.dtd");

			// create string from xml tree
			StreamResult result = new StreamResult(writer);
			DOMSource source = new DOMSource(doc);

			trans.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Document createMusicXmlDOM() {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = doc.createElement(SCORE_PARTWISE_TAG);
			doc.appendChild(root);
			doc.setXmlVersion("1.0");
			root.setAttribute("version", "2.0");

			Element movementNumberEl = doc.createElement(MOVEMENT_NUMBER_TAG);
			root.appendChild(movementNumberEl);
			Element movementTitleEl = doc.createElement(MOVEMENT_TITLE_TAG);
			if (score.metadata != null) {
				movementTitleEl.appendChild(doc.createTextNode(score.metadata.title));
			}
			root.appendChild(movementTitleEl);

			Element identificationEl = doc.createElement(IDENTIFICATION_TAG);
			if (score.metadata != null) {
				Element creatorEl = doc.createElement(CREATOR_TAG);
				creatorEl.setAttribute(TYPE_ATTRIBUTE, "composer");
				creatorEl.appendChild(doc.createTextNode(score.metadata.author));
				identificationEl.appendChild(creatorEl);
				creatorEl = doc.createElement(CREATOR_TAG);
				creatorEl.setAttribute(TYPE_ATTRIBUTE, "poet");
				creatorEl.appendChild(doc.createTextNode(score.metadata.lyricist));
				identificationEl.appendChild(creatorEl);

				Element rightsElement = doc.createElement(RIGHTS_TAG);
				rightsElement.appendChild(doc.createTextNode(score.metadata.copyright));
				identificationEl.appendChild(rightsElement);
			}

			Element encodingEl = doc.createElement(ENCODING_TAG);
			Element softwareEl = doc.createElement(SOFTWARE_TAG);
			softwareEl.appendChild(doc.createTextNode("NWC2MusicXML"));
			encodingEl.appendChild(softwareEl);
			Element encodingDateEl = doc.createElement(ENCODING_DATE_TAG);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			encodingDateEl.appendChild(doc.createTextNode(sdf.format(new Date())));
			encodingEl.appendChild(encodingDateEl);
			identificationEl.appendChild(encodingEl);
			root.appendChild(identificationEl);

			Element partListEl = doc.createElement(PART_LIST_TAG);
			int pId = 1;
			for (Part part : score.parts) {
				if (part.isVisible()) {
					Element scorePartEl = doc.createElement(SCORE_PART_TAG);
					scorePartEl.setAttribute(ID_ATTRIBUTE, "P" + pId);
					Element partNameEl = doc.createElement(PART_NAME_TAG);
					if (part.name != null)
						partNameEl.appendChild(doc.createTextNode(part.name));
					scorePartEl.appendChild(partNameEl);

					if (part.channel != null && part.program != -1) {
						Element midiInstrumentEl = doc.createElement(MIDI_INSTRUMENT_TAG);
						midiInstrumentEl.setAttribute(ID_ATTRIBUTE, "P" + pId);
						Element midiChannelEl = doc.createElement(MIDI_CHANNEL_TAG);
						midiChannelEl.appendChild(doc.createTextNode(part.channel));
						midiInstrumentEl.appendChild(midiChannelEl);
						Element midiProgramEl = doc.createElement(MIDI_PROGRAM_TAG);
						midiProgramEl.appendChild(doc.createTextNode(String.valueOf(part.program + 1)));
						midiInstrumentEl.appendChild(midiProgramEl);
						scorePartEl.appendChild(midiInstrumentEl);
					}

					partListEl.appendChild(scorePartEl);
					pId++;
				}
			}
			root.appendChild(partListEl);

			pId = 1;
			for (Part part : score.parts) {
				if (part.isVisible()) {
					Element partEl = doc.createElement(PART_TAG);
					partEl.setAttribute(ID_ATTRIBUTE, "P" + pId);
					root.appendChild(partEl);

					Element attributesEl = null;
					int attributesMeasure = -1;

					int measureCount = part.staves.get(0).length();
					for (int mId = 0; mId < measureCount; mId++) {
						int stId = 1;
						Element measureEl = doc.createElement(MEASURE_TAG);
						measureEl.setAttribute(NUMBER_ATTRIBUTE, "" + (mId + 1));
						partEl.appendChild(measureEl);
						attributesEl = null;
						Measure m0 = part.staves.get(0).measures.get(mId);
						if (part.staves.get(0).measures.get(mId).lineBreak) {
							Element printEl = doc.createElement(PRINT_TAG);
							printEl.setAttribute(NEWSYSTEM_ATTRIBUTE, "yes");
							measureEl.appendChild(printEl);
						}
						// bar
						if (m0.leftBarType != null) {
							if (m0.leftBarType.compareTo("light-light") == 0
									|| m0.leftBarType.compareTo("heavy-light") == 0) {
								Element barLineEl = doc.createElement(BAR_LINE_TAG);
								barLineEl.setAttribute(LOCATION_ATTRIBUTE, "left");
								Element barStyleEl = doc.createElement(BAR_STYLE_TAG);
								barStyleEl.appendChild(doc.createTextNode(m0.leftBarType));
								// Check whether ending tag of barline has
								// already been created, if so add before
								Node testEnding = barLineEl.getFirstChild();
								if (testEnding != null && (testEnding.getNodeName().compareTo(BAR_STYLE_TAG) != 0)) {
									barLineEl.insertBefore(barStyleEl, testEnding);
								} else {
									barLineEl.appendChild(barStyleEl);
								}
								if (m0.leftRepeat) {
									Element repeatElement = doc.createElement(REPEAT_TAG);
									repeatElement.setAttribute(DIRECTION_ATTRIBUTE, "forward");
									barLineEl.appendChild(repeatElement);
								}
								measureEl.appendChild(barLineEl);
							}
						}

						int backupStaff = 0;
						int backupVoice = 0;
						for (Staff staff : part.staves) {
							// allow graceful handling of staves with different
							// numbers of measures - without this check
							// get ArrayList.rangeCheck exception below on
							// staff.measures.get(mId)
							// this will produce an xml file with fewer bars on
							// subsequent stave(s) than the first stave
							// but better that than for the programme to crash
							// with an Exception
							if (mId >= staff.measures.size()) {
								break;
							}

							boolean addedNote = false;
							int[] noteKeys = staff.transformKeyListForTie();
							// slurStarted = false;
							if (backupStaff != 0) {
								Element backupEl = doc.createElement(BACKUP_TAG);
								Element durationEl = doc.createElement(DURATION_TAG);
								durationEl.appendChild(doc.createTextNode(String.valueOf(backupStaff)));
								backupEl.appendChild(durationEl);
								measureEl.appendChild(backupEl);
								backupStaff = 0;
							}
							Measure m = staff.measures.get(mId);
							TreeSet<Integer> set = new TreeSet<Integer>(m.voices.keySet());
							for (int voiceId : set) {
								if (backupVoice != 0) {
									Element backupEl = doc.createElement(BACKUP_TAG);
									Element durationEl = doc.createElement(DURATION_TAG);
									durationEl.appendChild(doc.createTextNode(String.valueOf(backupVoice)));
									backupEl.appendChild(durationEl);
									measureEl.appendChild(backupEl);
									backupVoice = 0;
								}
								ArrayList<IElement> v = m.voices.get(voiceId);
								Element carriedNotationsEl = null;

								for (IElement element : v) {
									// some directions require notation to be
									// added to the next note - this object
									// holds the notation
									if (element instanceof Clef) {
										Clef clef = (Clef) element;
										staff.currentClef = clef;
										if (attributesEl == null) {
											// create a new attribute element
											attributesEl = createMeasureGeneralAttributes(doc, clef, null, null,
													part.staves.size(), stId);
											measureEl.appendChild(attributesEl);
											attributesMeasure = mId;
										} else {
											if (mId == attributesMeasure) {
												if (stId > 1) {
													appendClefToAttribute(attributesEl, clef, doc, stId);
												}
											} else {
												attributesEl = createMeasureGeneralAttributes(doc, staff.currentClef,
														null, null, part.staves.size(), stId);
												measureEl.appendChild(attributesEl);
												attributesMeasure = mId;
											}
										}
									} else if (element instanceof TimeSig) {
										TimeSig timeSig = (TimeSig) element;
										if (attributesEl == null) {
											if (!addedNote) {
												// need a new attribute element
												attributesEl = createMeasureGeneralAttributes(doc, null, null, timeSig,
														part.staves.size(), stId);
												measureEl.appendChild(attributesEl);
												attributesMeasure = mId;
											} else {
												// note added -> create a new
												// attribute element for next
												// measure?
											}
										} else {
											// append to the existing attribute
											// element
											appendTo(attributesEl, timeSig, doc);
										}
										part.currentTimeSig = timeSig;
									} else if (element instanceof Key) {
										Key key = (Key) element;
										if (attributesEl == null) {
											if (!addedNote) {
												// need a new attribute element
												attributesEl = createMeasureGeneralAttributes(doc, null, key, null,
														part.staves.size(), stId);
												measureEl.appendChild(attributesEl);
												attributesMeasure = mId;
											} else {
												// note added -> create a new
												// attribute element for next
												// measure?
											}
										} else {
											// append to the existing attribute
											// element
											appendTo(attributesEl, key, doc);
										}
										staff.currentKey = key;

									} else if (element instanceof Wedge) {
										Element el = ((Wedge) element).toElement(doc);
										measureEl.appendChild(el);
									} else if (element instanceof TempoVariance) {
										TempoVariance tempoVariance = (TempoVariance) element;
										// both Fermata and Breath Mark apply to
										// the following note
										carriedNotationsEl = doc.createElement(NOTATIONS_TAG);
										if ((tempoVariance.type.compareTo("Breath Mark") == 0)) {
											Element articulationsEl = doc.createElement(ARTICULATIONS_TAG);
											Element breathMarkEl = doc.createElement(BREATH_MARK_TAG);
											articulationsEl.appendChild(breathMarkEl);
											carriedNotationsEl.appendChild(articulationsEl);
											// this element is picked up in Note
											// later on
										} else if ((tempoVariance.type.compareTo("Fermata") == 0)) {
											Element fermataEl = doc.createElement(FERMATA_TAG);
											carriedNotationsEl.appendChild(fermataEl);
											// this element is picked up in Note
											// later on
										} else {
											Element directionEl = doc.createElement(DIRECTION_TAG);
											Element directionTypeEl = doc.createElement(DIRECTIONTYPE_TAG);
											// haven't pulled any placement
											// through - but could
											// directionEl.setAttribute(PLACEMENT_ATTRIBUTE,
											// tempo.getPlacement());
											Element wordsEl = doc.createElement(WORDS_TAG);
											wordsEl.appendChild(doc.createTextNode(tempoVariance.getTempoVariance()));
											directionTypeEl.appendChild(wordsEl);
											directionEl.appendChild(directionTypeEl);
											measureEl.appendChild(directionEl);
										}
									} else if (element instanceof Note) {
										if (attributesEl == null && mId == 0) {
											attributesEl = createMeasureGeneralAttributes(doc, staff.currentClef, null,
													null, part.staves.size(), stId);
											measureEl.appendChild(attributesEl);
											attributesMeasure = mId;
										}
										Note note = (Note) element;
										// full rest
										int fullRestDuration = -1;
										if (m.isFullRest()) {
											fullRestDuration = (part.currentTimeSig.getBeats() * 4 / part.currentTimeSig
													.getBeatType()) * DIVISIONS_PER_QUARTER_NOTE;
										}
										Element noteEl = convert(doc, note, stId, voiceId, staff, noteKeys,
												fullRestDuration);
										if (carriedNotationsEl != null) {
											noteEl.appendChild(carriedNotationsEl);
											carriedNotationsEl = null;
										}
										measureEl.appendChild(noteEl);

										if (note.firstInChord)
											backupVoice += note.getDuration();

										addedNote = true;
									} else if (element instanceof Forward) {
										Forward forward = (Forward) element;
										Element forwardEl = doc.createElement(FORWARD_TAG);
										Element durationEl = doc.createElement(DURATION_TAG);
										durationEl
												.appendChild(doc.createTextNode(String.valueOf(forward.getDuration())));
										forwardEl.appendChild(durationEl);
										measureEl.appendChild(forwardEl);
										backupVoice += forward.getDuration();
									} else if (element instanceof Tempo) {
										Tempo tempo = (Tempo) element;
										Element directionEl = doc.createElement(DIRECTION_TAG);
										Element directionTypeEl = doc.createElement(DIRECTIONTYPE_TAG);
										directionEl.setAttribute(PLACEMENT_ATTRIBUTE, tempo.getPlacement());
										if (tempo.hasText()) {
											Element wordsEl = doc.createElement(WORDS_TAG);
											wordsEl.appendChild(doc.createTextNode(tempo.getText()));
											directionTypeEl.appendChild(wordsEl);
										} else {
											Element metronomeEl = doc.createElement(METRONOME_TAG);
											Element beatUnitEl = doc.createElement(BEAT_UNIT_TAG);
											beatUnitEl.appendChild(doc.createTextNode(tempo.getBaseLen()));
											metronomeEl.appendChild(beatUnitEl);
											if (tempo.isDotted()) {
												Element beatUnitDotEl = doc.createElement(BEAT_UNIT_DOT_TAG);
												metronomeEl.appendChild(beatUnitDotEl);
											}
											Element perMinuteEl = doc.createElement(PER_MINUTE_TAG);
											perMinuteEl.appendChild(doc.createTextNode(Integer.toString(tempo
													.getTempo())));
											metronomeEl.appendChild(perMinuteEl);
											directionTypeEl.appendChild(metronomeEl);
										}
										directionEl.appendChild(directionTypeEl);
										Element soundEl = doc.createElement(SOUND_TAG);
										soundEl.setAttribute(TEMPO_ATTRIBUTE, tempo.getAbsoluteTempo());
										directionEl.appendChild(soundEl);
										measureEl.appendChild(directionEl);
									} else if (element instanceof Text) {
										Text text = (Text) element;
										Element directionEl = doc.createElement(DIRECTION_TAG);
										Element directionTypeEl = doc.createElement(DIRECTIONTYPE_TAG);
										Element wordsEl = doc.createElement(WORDS_TAG);
										wordsEl.appendChild(doc.createTextNode(text.text));
										directionTypeEl.appendChild(wordsEl);
										directionEl.appendChild(directionTypeEl);
										if (part.staves.size() > 1) {
											Element staffEl = doc.createElement(STAFF_TAG);
											staffEl.appendChild(doc.createTextNode(String.valueOf(stId)));
											directionEl.appendChild(staffEl);
										}
										measureEl.appendChild(directionEl);
									} else if (element instanceof Dynamics) {
										Dynamics dynamics = (Dynamics) element;
										Element directionEl = doc.createElement(DIRECTION_TAG);
										Element directionTypeEl = doc.createElement(DIRECTIONTYPE_TAG);
										Element dynamicsElement = doc.createElement(DYNAMICS_TAG);
										Element typeEl = doc.createElement(dynamics.text);
										dynamicsElement.appendChild(typeEl);
										directionTypeEl.appendChild(dynamicsElement);
										directionEl.appendChild(directionTypeEl);
										if (part.staves.size() > 1) {
											Element staffEl = doc.createElement(STAFF_TAG);
											staffEl.appendChild(doc.createTextNode(String.valueOf(stId)));
											directionEl.appendChild(staffEl);
										}
										measureEl.appendChild(directionEl);
									} else if (element instanceof Flow) {
										Flow flow = (Flow) element;
										Element directionEl = doc.createElement(DIRECTION_TAG);
										Element directionTypeEl = doc.createElement(DIRECTIONTYPE_TAG);
										Element flowEl;
										if (flow.getWords() != "") {
											flowEl = doc.createElement(WORDS_TAG);
											flowEl.appendChild(doc.createTextNode(flow.getWords()));
										} else if (flow.getTag() != "") {
											flowEl = doc.createElement(flow.getTag());
										} else {
											// can't happen the way Flow is set
											// up
											flowEl = doc.createElement("dummy");
										}
										directionTypeEl.appendChild(flowEl);
										directionEl.appendChild(directionTypeEl);
										Element voiceEl = doc.createElement(VOICE_TAG);
										voiceEl.appendChild(doc.createTextNode(String.valueOf(voiceId)));
										directionEl.appendChild(voiceEl);
										Element staffEl = doc.createElement(STAFF_TAG);
										staffEl.appendChild(doc.createTextNode(new Integer(backupStaff + 1).toString()));
										directionEl.appendChild(staffEl);

										if (flow.getSoundNodeNum() != "") {
											Element soundEl = doc.createElement(SOUND_TAG);
											soundEl.setAttribute(flow.getSoundNodeNum(), String.valueOf(mId));
											directionEl.appendChild(soundEl);
										}
										if (flow.getSoundNodeYes() != "") {
											Element soundEl = doc.createElement(SOUND_TAG);
											soundEl.setAttribute(flow.getSoundNodeYes(), "yes");
											directionEl.appendChild(soundEl);
										}

										measureEl.appendChild(directionEl);
									} else if (element instanceof Ending) {
										Ending ending = (Ending) element;
										boolean leftBarlineSought = false;
										if (ending.type.compareTo("start") == 0) {
											leftBarlineSought = true;
										}
										// check if last ending in a set - then
										// need to discontinue, not stop
										if (ending.lastInSet && (ending.type.compareTo("stop") == 0)) {
											ending.type = "discontinue";
										}
										// create the Ending Element that we'll
										// add when we work out where
										Element endingEl = doc.createElement(ENDING_TAG);
										endingEl.setAttribute(NUMBER_ATTRIBUTE, ending.number);
										endingEl.setAttribute(TYPE_ATTRIBUTE, ending.type);

										// Need to work out if the barline we
										// need is already there or whether we
										// need to create it
										NodeList barlineEls = measureEl.getElementsByTagName(BAR_LINE_TAG);
										Element leftBarlineEl = null;
										Element rightBarlineEl = null;
										// if there are barlines
										if (barlineEls.getLength() > 0) {
											for (int i = 0; i < barlineEls.getLength(); i++) {
												Node barline = barlineEls.item(i);
												Node attrLocation = barline.getAttributes().getNamedItem(
														LOCATION_ATTRIBUTE);
												if (attrLocation != null) {
													if (attrLocation.getTextContent().compareTo("right") == 0
															&& !leftBarlineSought) {
														// right barline - what
														// we need for stop and
														// discontinue types
														rightBarlineEl = (Element) barline;
														break;
													} else if (attrLocation.getTextContent().compareTo("left") == 0
															&& leftBarlineSought) {
														// left barline - what
														// we need for start
														// type
														leftBarlineEl = (Element) barline;
														break;
													}
												}
											}
										}
										// if we haven't found an existing
										// barline element, then create one and
										// add it to the measure
										if (leftBarlineSought && leftBarlineEl == null) {
											leftBarlineEl = doc.createElement(BAR_LINE_TAG);
											leftBarlineEl.setAttribute(LOCATION_ATTRIBUTE, "left");
											// will this add it in the wrong
											// place - given it's already been
											// worked through?
											measureEl.appendChild(leftBarlineEl);
										} else if (!leftBarlineSought && rightBarlineEl == null) {
											rightBarlineEl = doc.createElement(BAR_LINE_TAG);
											rightBarlineEl.setAttribute(LOCATION_ATTRIBUTE, "right");
											measureEl.appendChild(rightBarlineEl);
										}
										// now add the Element ending to the
										// barline
										if (leftBarlineEl != null) {
											leftBarlineEl.appendChild(endingEl);
										} else if (rightBarlineEl != null) {
											rightBarlineEl.appendChild(endingEl);
										}
									}
								}
							}
							// m.voice1
							stId++;
							backupStaff = backupVoice;
							backupVoice = 0;
						}
						// bar
						if (m0.rightBarType != null) {
							if (m0.rightBarType.compareTo("light-light") == 0
									|| m0.rightBarType.compareTo("light-heavy") == 0) {
								// because an ending (above) may already have
								// created the right barline - need loop to
								// check
								NodeList barlineEls = measureEl.getElementsByTagName(BAR_LINE_TAG);
								Element barlineEl = null;
								boolean alreadyInMeasure = false;
								// find right barline, if any capture the
								// reference
								if (barlineEls.getLength() > 0) {
									for (int i = 0; i < barlineEls.getLength(); i++) {
										Node barline = barlineEls.item(i);
										Node attrLocation = barline.getAttributes().getNamedItem(LOCATION_ATTRIBUTE);
										if (attrLocation != null
												&& attrLocation.getTextContent().compareTo("right") == 0) {
											barlineEl = (Element) barline;
											alreadyInMeasure = true;
											break;
										}
									}
								}
								// didn't find one, so create right barline
								if (barlineEl == null) {
									barlineEl = doc.createElement(BAR_LINE_TAG);
									barlineEl.setAttribute(LOCATION_ATTRIBUTE, "right");
								}
								Element barStyleEl = doc.createElement(BAR_STYLE_TAG);
								barStyleEl.appendChild(doc.createTextNode(m0.rightBarType));
								// Check whether ending tag of barline has
								// already been created, if so add before
								Node testEnding = barlineEl.getFirstChild();
								if (testEnding != null && (testEnding.getNodeName().compareTo(BAR_STYLE_TAG) != 0)) {
									barlineEl.insertBefore(barStyleEl, testEnding);
								} else {
									barlineEl.appendChild(barStyleEl);
								}
								if (m0.rightRepeat) {
									Element repeatElement = doc.createElement(REPEAT_TAG);
									repeatElement.setAttribute(DIRECTION_ATTRIBUTE, "backward");
									// new repeats insert
									if (m0.repeatTimes != null) {
										repeatElement.setAttribute(REPEAT_TIMES_TAG, m0.repeatTimes);
									}
									barlineEl.appendChild(repeatElement);
									// add words direction to play n times in
									// before barline
									if (m0.repeatTimes != null) {
										Element directionEl = doc.createElement(DIRECTION_TAG);
										Element directionTypeEl = doc.createElement(DIRECTIONTYPE_TAG);
										Element wordsEl = doc.createElement(WORDS_TAG);
										wordsEl.appendChild(doc.createTextNode("Play " + m0.repeatTimes + " times"));
										directionTypeEl.appendChild(wordsEl);
										directionEl.appendChild(directionTypeEl);
										measureEl.appendChild(directionEl);
									}
								}
								if (!alreadyInMeasure) {
									// don't need to add to measure, because it
									// was already there
									measureEl.appendChild(barlineEl);
								}
							}
						}
						if (mId == 0 && attributesEl != null && part.trans != 0) {
							Element transposeEl = doc.createElement(TRANSPOSE_TAG);
							Element chromaticEl = doc.createElement(CHROMATIC_TAG);
							chromaticEl.appendChild(doc.createTextNode(String.valueOf(part.trans)));
							transposeEl.appendChild(chromaticEl);
							attributesEl.appendChild(transposeEl);
						}
						if (mId == measureCount - 1) {
							// final barline
							NodeList barlineEls = measureEl.getElementsByTagName(BAR_LINE_TAG);
							Node barlineEl = null;
							// find right barline, if any do nothing
							if (barlineEls.getLength() > 0) {
								for (int i = 0; i < barlineEls.getLength(); i++) {
									Node barline = barlineEls.item(i);
									Node attrLocation = barline.getAttributes().getNamedItem(LOCATION_ATTRIBUTE);
									if (attrLocation != null && attrLocation.getTextContent().compareTo("right") == 0) {
										barlineEl = barline;
										break;
									}
								}
							}
							// didn't find one, so create right barline
							if (barlineEl == null) {
								Element barLineEl = doc.createElement(BAR_LINE_TAG);
								barLineEl.setAttribute(LOCATION_ATTRIBUTE, "right");
								Element barStyleEl = doc.createElement(BAR_STYLE_TAG);
								barStyleEl.appendChild(doc.createTextNode(p.barLineStyle));
								// Check whether ending tag of barline has
								// already been created, if so add before
								Node testEnding = barLineEl.getFirstChild();
								if (testEnding != null && (testEnding.getNodeName().compareTo(BAR_STYLE_TAG) != 0)) {
									barLineEl.insertBefore(barStyleEl, testEnding);
								} else {
									barLineEl.appendChild(barStyleEl);
								}
								if (p.endRepeat) {
									Element repeatElement = doc.createElement(REPEAT_TAG);
									repeatElement.setAttribute(DIRECTION_ATTRIBUTE, "backward");
									// new repeats insert
									if (m0.repeatTimes != null) {
										repeatElement.setAttribute(REPEAT_TIMES_TAG, m0.repeatTimes);
									}
									barLineEl.appendChild(repeatElement);
								}
								measureEl.appendChild(barLineEl);
							}
						}
					}
					pId++;
				}// part visible
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return doc;
	}

	protected Element convert(Document doc, Note note, int staffId, int voiceId, Staff staff, int[] noteKeys,
			int fullRestDuration) {

		Clef clef = staff.currentClef;
		Key key = staff.currentKey;
		Set<Integer> tieList = staff.tieList;
		boolean tieStop = false;
		boolean slurStart = false;
		boolean slurStop = false;
		boolean prevSlurIsGrace = false;
		if (note.slur()) {
			if (staff.slurStarted == null) {
				staff.slurStarted = note;
				slurStart = true;
			} else {
				slurStart = false;
			}
		} else if (!note.grace()) { // slur stop on normal note only
			if (staff.slurStarted != null) {
				prevSlurIsGrace = staff.slurStarted.grace();
				staff.slurStarted = null;
				slurStop = true;
			}
		}

		Element noteEl = doc.createElement(NOTE_TAG);
		Element durationEl = doc.createElement(DURATION_TAG);

		if (note.rest) {
			Element rest = doc.createElement(REST_TAG);
			noteEl.appendChild(rest);
		} else {
			if (note.grace()) {
				Element grace = doc.createElement(GRACE_TAG);
				noteEl.appendChild(grace);
			}
			String stepValue = note.getStep(clef);
			int octave = note.getOctave(clef);
			int alterValue = key.getAlterForStep(stepValue);
			int noteAlterValue = Key.getAlterForStep(stepValue, noteKeys);

			if (note.chord && !note.firstInChord) {
				Element rest = doc.createElement(CHORD_TAG);
				noteEl.appendChild(rest);
			}

			Element pitchEl = doc.createElement(PITCH_TAG);
			Element stepEl = doc.createElement(STEP_TAG);
			stepEl.appendChild(doc.createTextNode(stepValue));
			pitchEl.appendChild(stepEl);

			if (note.accidental || alterValue != 0 || noteAlterValue != 0) {
				if (note.accidental) {
					Element alterEl = doc.createElement(ALTER_TAG);
					alterEl.appendChild(doc.createTextNode(Integer.toString(note.alt)));
					pitchEl.appendChild(alterEl);
					int index = Key.SHARPS.indexOf(stepValue);
					if (note.alt == 0) {
						noteKeys[index] = Key.NATURAL; // keep track for tie!
					} else {
						noteKeys[index] = note.alt;
					}
				} else if (noteAlterValue == Key.NATURAL
						|| (noteAlterValue >= -2 && noteAlterValue <= 2 && noteAlterValue != 0)) {
					if (!(noteAlterValue == Key.NATURAL)) {
						Element alterEl = doc.createElement(ALTER_TAG);
						alterEl.appendChild(doc.createTextNode(Integer.toString(noteAlterValue)));
						pitchEl.appendChild(alterEl);
					}
				} else if (alterValue != 0) {
					Element alterEl = doc.createElement(ALTER_TAG);
					alterEl.appendChild(doc.createTextNode(Integer.toString(alterValue)));
					pitchEl.appendChild(alterEl);
				}

			}

			String octaveValue = new Integer(octave).toString();
			Element octaveEl = doc.createElement(OCTAVE_TAG);
			octaveEl.appendChild(doc.createTextNode(octaveValue));
			pitchEl.appendChild(octaveEl);

			noteEl.appendChild(pitchEl);

		}
		if (!note.grace()) {

			int relDuration = note.getDuration();
			if (fullRestDuration != -1) {
				relDuration = fullRestDuration;
			}
			durationEl.appendChild(doc.createTextNode(new Integer(relDuration).toString()));
			noteEl.appendChild(durationEl);
		}

		if (!note.rest) {
			String stepValue = note.getStep(clef);
			int step = "CDEFGABC".indexOf(stepValue);
			if (tieList.contains(step)) {
				Element tieEl = doc.createElement(TIE_TAG);
				tieEl.setAttribute(TYPE_ATTRIBUTE, "stop");
				noteEl.appendChild(tieEl);
				tieList.remove(step);
				tieStop = true;
			}
			if (note.startTie) {
				Element tieEl = doc.createElement(TIE_TAG);
				tieEl.setAttribute(TYPE_ATTRIBUTE, "start");
				noteEl.appendChild(tieEl);
				tieList.add(step);
			}
		}

		Element voiceElement = doc.createElement(VOICE_TAG);
		voiceElement.appendChild(doc.createTextNode(String.valueOf(voiceId)));
		noteEl.appendChild(voiceElement);

		// no type element for full rest
		if (fullRestDuration == -1 || !note.rest) {
			Node type = doc.createElement(TYPE_TAG);
			Node typeValue = doc.createTextNode(note.getType());

			if (typeValue != null) {
				type.appendChild(typeValue);
				noteEl.appendChild(type);
			}
		}

		if (note.dots() >= 1) {
			for (int i = 0; i < note.dots(); i++) {
				Node dot = doc.createElement(DOT_TAG);
				noteEl.appendChild(dot);
			}
		}

		if (note.accidental) {
			Node acc = doc.createElement(ACCIDENTAL_TAG);
			Node accValue = null;
			switch (note.alt) {
			case -2:
				accValue = doc.createTextNode("flat-flat");
				break;
			case -1:
				accValue = doc.createTextNode("flat");
				break;
			case 0:
				accValue = doc.createTextNode("natural");
				break;
			case 1:
				accValue = doc.createTextNode("sharp");
				break;
			case 2:
				accValue = doc.createTextNode("double-sharp");
				break;
			}
			acc.appendChild(accValue);
			noteEl.appendChild(acc);
		}

		if (note.isPartOfTriplet()) {
			Element timeModifEl = doc.createElement(TIMEMODIFICATION_TAG);
			Element actualEl = doc.createElement(ACTUALNOTES_TAG);
			actualEl.appendChild(doc.createTextNode("3"));
			timeModifEl.appendChild(actualEl);
			Element normalEl = doc.createElement(NORMALNOTES_TAG);
			normalEl.appendChild(doc.createTextNode("2"));
			timeModifEl.appendChild(normalEl);
			noteEl.appendChild(timeModifEl);
		}

		if (note.stem != null) {
			Element stemEl = doc.createElement(STEM_TAG);
			stemEl.appendChild(doc.createTextNode(note.stem));
			noteEl.appendChild(stemEl);
		}

		if (note.notehead != null) {
			Element noteheadEl = doc.createElement(NOTEHEAD_TAG);
			noteheadEl.appendChild(doc.createTextNode(note.notehead));
			if (note.notehead.equals("diamond"))
				noteheadEl.setAttribute("filled", "no");
			noteEl.appendChild(noteheadEl);
		}

		Element staffElement = doc.createElement(STAFF_TAG);
		staffElement.appendChild(doc.createTextNode(new Integer(staffId).toString()));
		noteEl.appendChild(staffElement);

		if (note.isBeamed() && note.firstInChord) {
			// Ideally, this section should look forward to the next note in the
			// beam, and do hook segments as needed (this is not currently done)
			int runFlags = staff.currentBeamCount;
			int newFlags = note.getStemFlagCount();
			int maxFlags = Math.max(runFlags, newFlags);
			boolean endBeam = note.isBeamEnd();

			for (int flag = 1; flag <= maxFlags; flag++) {
				String beamSeg = "end";
				if (endBeam) {
					if (flag > runFlags)
						beamSeg = "backward hook";
				} else if (flag > newFlags) {
					beamSeg = "end";
				} else if (flag > runFlags) {
					beamSeg = "begin";
				} else {
					beamSeg = "continue";
				}

				Element beamEl = doc.createElement(BEAM_TAG);
				beamEl.setAttribute(NUMBER_ATTRIBUTE, new Integer(flag).toString());
				beamEl.appendChild(doc.createTextNode(beamSeg));
				noteEl.appendChild(beamEl);
			}

			staff.currentBeamCount = endBeam ? 0 : newFlags;
		}

		Element notationsEl = null;
		if (note.startTie || tieStop) {
			notationsEl = doc.createElement(NOTATIONS_TAG);
			if (tieStop) {
				Element tiedElement = doc.createElement(TIED_TAG);
				tiedElement.setAttribute(TYPE_ATTRIBUTE, "stop");
				notationsEl.appendChild(tiedElement);
			}
			if (note.startTie) {
				Element tiedElement = doc.createElement(TIED_TAG);
				tiedElement.setAttribute(TYPE_ATTRIBUTE, "start");
				notationsEl.appendChild(tiedElement);
			}
			noteEl.appendChild(notationsEl);
		}

		if (note.tripletStart() || note.tripletEnd()) {
			if (notationsEl == null) {
				notationsEl = doc.createElement(NOTATIONS_TAG);
				noteEl.appendChild(notationsEl);
			}
			if (note.tripletStart()) {
				Element tupletElement = doc.createElement(TUPLET_TAG);
				tupletElement.setAttribute(TYPE_ATTRIBUTE, "start");
				notationsEl.appendChild(tupletElement);
			}
			if (note.tripletEnd()) {
				Element tupletElement = doc.createElement(TUPLET_TAG);
				tupletElement.setAttribute(TYPE_ATTRIBUTE, "stop");
				notationsEl.appendChild(tupletElement);
			}
		}
		if (slurStart || slurStop) {
			if (notationsEl == null) {
				notationsEl = doc.createElement(NOTATIONS_TAG);
				noteEl.appendChild(notationsEl);
			}
			if (slurStart) {
				Element slurElement = doc.createElement(SLUR_TAG);
				slurElement.setAttribute(TYPE_ATTRIBUTE, "start");
				slurElement.setAttribute(NUMBER_ATTRIBUTE, "1");
				notationsEl.appendChild(slurElement);
			}
			if (slurStop) {
				Element slurElement = doc.createElement(SLUR_TAG);
				slurElement.setAttribute(TYPE_ATTRIBUTE, "stop");
				slurElement.setAttribute(NUMBER_ATTRIBUTE, "1");
				notationsEl.appendChild(slurElement);
			}
		}

		if (note.firstInChord && (note.isAccent() || note.isStaccato() || note.isTenuto())) {
			if (notationsEl == null) {
				notationsEl = doc.createElement(NOTATIONS_TAG);
				noteEl.appendChild(notationsEl);
			}
			Element articulationsEl = doc.createElement(ARTICULATIONS_TAG);
			notationsEl.appendChild(articulationsEl);
			if (note.isAccent()) {
				articulationsEl.appendChild(doc.createElement(ACCENT_TAG));
			}
			if (note.isStaccato()) {
				articulationsEl.appendChild(doc.createElement(STACCATO_TAG));
			}
			if (note.isTenuto()) {
				articulationsEl.appendChild(doc.createElement(TENUTO_TAG));
			}
		}

		// lyrics
		if ((note.firstInChord && (voiceId % 4 == 1) && !note.rest && !note.isLyricNever() && !note.grace())
				|| note.isLyricAlways()) {
			if ((tieStop || (note.slur() && !slurStart) || (slurStop && !prevSlurIsGrace)) && !note.isLyricAlways()) {
				// nothing to do
			} else {
				int number = 1;
				for (Lyrics lyrics : staff.lyricsLine) {
					Lyric l = lyrics.next();
					if (l != null) {

						Element lyricEl = doc.createElement(LYRIC_TAG);
						if (staff.lyricsLine.size() > 1) {
							lyricEl.setAttribute(NUMBER_ATTRIBUTE, String.valueOf(number));
						}
						Element syllabicEl = doc.createElement(SYLLABIC_TAG);
						syllabicEl.appendChild(doc.createTextNode(l.syllabic));
						lyricEl.appendChild(syllabicEl);
						Element textEl = doc.createElement(TEXT_TAG);
						textEl.appendChild(doc.createTextNode(l.text));
						lyricEl.appendChild(textEl);
						if (l.endLine) {
							lyricEl.appendChild(doc.createElement(END_LINE_TAG));
						} else if (l.endParagraph) {
							lyricEl.appendChild(doc.createElement(END_PARAGRAPH_TAG));
						}
						noteEl.appendChild(lyricEl);
					}

					number++;
				}
			}
		}

		return noteEl;
	}

	public Element createMeasureGeneralAttributes(Document doc, Clef clef, Key key, TimeSig timeSig, int staffCount,
			int staffId) {
		Element attributeEl = doc.createElement(ATTRIBUTES_TAG);

		Element divisionEl = doc.createElement(DIVISIONS_TAG);
		divisionEl.appendChild(doc.createTextNode(new Integer(DIVISIONS_PER_QUARTER_NOTE).toString()));
		attributeEl.appendChild(divisionEl);

		if (key != null) {
			attributeEl.appendChild(convert(key, doc));
		}

		if (timeSig != null) {
			attributeEl.appendChild(convert(timeSig, doc));
		}

		if (staffCount > 1) {
			Element stavesEl = doc.createElement(STAVES_TAG);
			stavesEl.appendChild(doc.createTextNode(String.valueOf(staffCount)));
			attributeEl.appendChild(stavesEl);
		}
		if (clef != null) {
			Element clefEl = doc.createElement(CLEF_TAG);
			if (staffCount > 1)
				clefEl.setAttribute(NUMBER_ATTRIBUTE, String.valueOf(staffId));
			Element signEl = doc.createElement(SIGN_TAG);
			signEl.appendChild(doc.createTextNode(clef.getSign()));
			Element linEl = doc.createElement(LINE_TAG);
			linEl.appendChild(doc.createTextNode(clef.getLine()));
			clefEl.appendChild(signEl);
			clefEl.appendChild(linEl);
			if (clef.octaveShift != 0) {
				Element octaveChangeEl = doc.createElement(CLEF_OCTAVE_CHANGE_TAG);
				octaveChangeEl.appendChild(doc.createTextNode(String.valueOf(clef.octaveShift)));
				clefEl.appendChild(octaveChangeEl);
			}
			attributeEl.appendChild(clefEl);
		}
		return attributeEl;
	}

	protected void appendClefToAttribute(Element attributeEl, Clef clef, Document doc, int staffId) {
		attributeEl.appendChild(convert(clef, staffId, doc));
	}

	protected void appendTo(Element attributeEl, TimeSig timeSig, Document doc) {
		NodeList stavesEl = attributeEl.getElementsByTagName(STAVES_TAG);
		if (stavesEl.getLength() > 0) {
			// if there is a stave_tag then this is a multiple-stave part and
			// need to take care not to add TimeSig twice
			NodeList prevTimeSigEl = attributeEl.getElementsByTagName(TIME_TAG);
			if (prevTimeSigEl.getLength() == 0) {
				attributeEl.insertBefore(convert(timeSig, doc), stavesEl.item(0));
			}
		} else {
			NodeList clef = attributeEl.getElementsByTagName(CLEF_TAG);
			if (clef.getLength() > 0) {
				attributeEl.insertBefore(convert(timeSig, doc), clef.item(0));
			} else {
				attributeEl.appendChild(convert(timeSig, doc));
			}
		}
	}

	protected void appendTo(Element attributeEl, Key key, Document doc) {
		NodeList stavesEl = attributeEl.getElementsByTagName(STAVES_TAG);
		if (stavesEl.getLength() > 0) {
			// if there is a stave_tag then this is a multiple-stave part and
			// need to take care not to add Key twice
			NodeList prevKeyEl = attributeEl.getElementsByTagName(KEY_TAG);
			if (prevKeyEl.getLength() == 0) {
				attributeEl.insertBefore(convert(key, doc), stavesEl.item(0));
			}
		} else {
			NodeList clef = attributeEl.getElementsByTagName(CLEF_TAG);
			if (clef.getLength() > 0) {
				attributeEl.insertBefore(convert(key, doc), clef.item(0));
			} else {
				attributeEl.appendChild(convert(key, doc));
			}
		}
	}

	protected Element convert(TimeSig timeSig, Document doc) {
		Element timeEl = doc.createElement(TIME_TAG);
		Element beatsEl = doc.createElement(BEATS_TAG);
		Element beatTypeEl = doc.createElement(BEAT_TYPE_TAG);
		beatsEl.appendChild(doc.createTextNode(Integer.toString(timeSig.getBeats())));
		beatTypeEl.appendChild(doc.createTextNode(Integer.toString(timeSig.getBeatType())));

		timeEl.appendChild(beatsEl);
		timeEl.appendChild(beatTypeEl);
		String symbol = timeSig.getSymbol();
		if (symbol != null) {
			timeEl.setAttribute(SYMBOL_ATTRIBUTE, symbol);
		}
		return timeEl;
	}

	protected Element convert(Clef clef, int staffId, Document doc) {
		Element clefEl = doc.createElement(CLEF_TAG);
		clefEl.setAttribute(NUMBER_ATTRIBUTE, String.valueOf(staffId));
		Element signEl = doc.createElement(SIGN_TAG);
		signEl.appendChild(doc.createTextNode(clef.getSign()));
		Element linEl = doc.createElement(LINE_TAG);
		linEl.appendChild(doc.createTextNode(clef.getLine()));
		clefEl.appendChild(signEl);
		clefEl.appendChild(linEl);
		if (clef.octaveShift != 0) {
			Element octaveChangeEl = doc.createElement(CLEF_OCTAVE_CHANGE_TAG);
			octaveChangeEl.appendChild(doc.createTextNode(String.valueOf(clef.octaveShift)));
			clefEl.appendChild(octaveChangeEl);
		}
		return clefEl;
	}

	protected Node convert(Key key, Document doc) {
		Element keyEl = doc.createElement(KEY_TAG);
		Element fifthEl = doc.createElement(FIFTHS_TAG);

		fifthEl.appendChild(doc.createTextNode(String.valueOf(key.getFifth())));

		keyEl.appendChild(fifthEl);
		Element modeEl = doc.createElement(MODE_TAG);
		modeEl.appendChild(doc.createTextNode("major"));

		keyEl.appendChild(modeEl);
		return keyEl;
	}

	public static void main(String[] args) {
		if ((args.length == 1) && (args[0].equalsIgnoreCase("-ut"))) {
			Nwc2MusicXML converter = new Nwc2MusicXML();
			ConversionResult result = converter.convert(System.in);
			String title = result.getTitle();
			if (converter.write(System.out) == -1) {
				System.err.println("Error while converting [" + title + "]");
			}

			System.exit(99);
		} else if ((args.length == 1) && (args[0].equalsIgnoreCase("-utsave"))) {
			Nwc2MusicXML converter = new Nwc2MusicXML();
			ConversionResult result = converter.convert(System.in);
			String title = result.getTitle();
			JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileNameExtensionFilter("MusicXML", "xml"));
			fc.setSelectedFile(new File(title + ".xml"));
			int returnVal = fc.showSaveDialog(null);

			if (returnVal != JFileChooser.APPROVE_OPTION) {
				System.out.println("Aborted");
				System.exit(99);
			}

			File out = fc.getSelectedFile();

			try {
				if (converter.write(new FileOutputStream(out)) == -1) {
					System.out.println("Error while converting [" + title + "]");
				} else {
					System.out.println("Success\n\n-> " + out.getAbsolutePath());
				}
			} catch (FileNotFoundException e) {
				System.out.println("Output file [" + out.getAbsolutePath() + "] exception");
			}

			System.exit(99);
		} else if (args.length == 2) {
			File in = new File(args[0]);
			File out = new File(args[1]);
			if (in.exists()) {
				try {
					FileInputStream inStream = new FileInputStream(in);
					Nwc2MusicXML converter = new Nwc2MusicXML();
					ConversionResult result = converter.convert(inStream);
					String title = result.getTitle();
					System.err.println("Converting... title: [" + title + "]");
					if (!result.isError()) {
						if (converter.write(new FileOutputStream(out)) == -1) {
							System.err.println("Error while converting [" + title + "]");
						} else {
							System.err.println("Success !  [" + out.getAbsolutePath() + "]");
						}
					} else {
						if (result.getErrorCode() == ConversionResult.ERROR_OLD_VERSION) {
							System.err
									.println("Error: NWC file version is < 2.75. The converter only supports NWC 2.75+ files");
						} else {
							System.err.println("Conversion error");
						}
					}
				} catch (FileNotFoundException e) {
					System.err.println("File " + in.getAbsolutePath() + " not found");
				}
			} else {
				System.err.println("File " + in.getAbsolutePath() + " not found");
			}

		} else {
			System.err
					.println("usage : java -cp . nwc2musicxml.jar fr.lasconic.convert.nwc2musicxml.Nwc2MusicXML file.nwctxt myfile.xml");

		}
	}

}

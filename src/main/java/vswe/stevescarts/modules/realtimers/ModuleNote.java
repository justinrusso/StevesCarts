package vswe.stevescarts.modules.realtimers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import org.lwjgl.opengl.GL11;
import vswe.stevescarts.entitys.EntityMinecartModular;
import vswe.stevescarts.guis.GuiMinecart;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;
import vswe.stevescarts.modules.ModuleBase;

import java.util.ArrayList;

public class ModuleNote extends ModuleBase {
	private final int maximumTracksPerModuleBitCount = 4;
	private final int maximumNotesPerTrackBitCount = 12;
	private int veryLongTrackLimit;
	private int notesInView;
	private int tracksInView;
	private int[] instrumentColors;
	private String[] pitchNames;
	private Localization.MODULES.ATTACHMENTS[] instrumentNames;
	private ArrayList<Track> tracks;
	private int notemapX;
	private int notemapY;
	private int trackHeight;
	private ArrayList<Button> buttons;
	private ArrayList<Button> instrumentbuttons;
	private int currentInstrument;
	private Button createTrack;
	private Button removeTrack;
	private Button speedButton;
	private boolean isScrollingX;
	private boolean isScrollingXTune;
	private int scrollX;
	private boolean isScrollingY;
	private int scrollY;
	private int pixelScrollX;
	private int pixelScrollXTune;
	private int generatedScrollX;
	private int pixelScrollY;
	private int generatedScrollY;
	private int[] scrollXrect;
	private int[] scrollYrect;
	private final int maximumNotesPerTrack;
	private final int maximumTracksPerModule;
	private int currentTick;
	private int playProgress;
	private boolean tooLongTrack;
	private boolean tooTallModule;
	private boolean veryLongTrack;
	private int speedSetting;
	private short lastModuleHeader;

	private DataParameter<Boolean> PLAYING;

	public ModuleNote(final EntityMinecartModular cart) {
		super(cart);
		veryLongTrackLimit = 1024;
		notesInView = 13;
		tracksInView = 5;
		instrumentColors = new int[] { 4210752, 16711680, 65280, 255, 16776960, 65535 };
		pitchNames = new String[] { "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#" };
		instrumentNames = new Localization.MODULES.ATTACHMENTS[] { Localization.MODULES.ATTACHMENTS.PIANO, Localization.MODULES.ATTACHMENTS.BASS_DRUM, Localization.MODULES.ATTACHMENTS.SNARE_DRUM,
			Localization.MODULES.ATTACHMENTS.STICKS, Localization.MODULES.ATTACHMENTS.BASS_GUITAR };
		notemapX = 70;
		notemapY = 40;
		trackHeight = 20;
		currentInstrument = -1;
		scrollXrect = new int[] { notemapX + 120, notemapY - 20, 100, 16 };
		scrollYrect = new int[] { notemapX + 220, notemapY, 16, 100 };
		currentTick = 0;
		playProgress = 0;
		tooLongTrack = false;
		tooTallModule = false;
		veryLongTrack = false;
		speedSetting = 5;
		maximumNotesPerTrack = (int) Math.pow(2.0, 12.0) - 1;
		maximumTracksPerModule = (int) Math.pow(2.0, 4.0) - 1;
		tracks = new ArrayList<>();
		if (getCart().world.isRemote) {
			buttons = new ArrayList<>();
			createTrack = new Button(notemapX - 60, notemapY - 20);
			createTrack.text = Localization.MODULES.ATTACHMENTS.CREATE_TRACK.translate();
			createTrack.imageID = 0;
			removeTrack = new Button(notemapX - 40, notemapY - 20);
			removeTrack.text = Localization.MODULES.ATTACHMENTS.REMOVE_TRACK.translate();
			removeTrack.imageID = 1;
			speedButton = new Button(notemapX - 20, notemapY - 20);
			updateSpeedButton();
			instrumentbuttons = new ArrayList<>();
			for (int i = 0; i < 6; ++i) {
				final Button tempButton = new Button(notemapX - 20 + (i + 1) * 20, notemapY - 20);
				instrumentbuttons.add(tempButton);
				if (i > 0) {
					tempButton.text = Localization.MODULES.ATTACHMENTS.ACTIVATE_INSTRUMENT.translate(instrumentNames[i - 1].translate(new String[0]));
				} else {
					tempButton.text = Localization.MODULES.ATTACHMENTS.DEACTIVATE_INSTRUMENT.translate();
				}
				tempButton.color = instrumentColors[i];
			}
		}
	}

	private void updateSpeedButton() {
		if (getCart().world.isRemote) {
			speedButton.imageID = 14 - speedSetting;
			speedButton.text = Localization.MODULES.ATTACHMENTS.NOTE_DELAY.translate(String.valueOf(getTickDelay()));
		}
	}

	@Override
	public void drawForeground(final GuiMinecart gui) {
		drawString(gui, getModuleName(), 8, 6, 4210752);
		for (int i = getScrollY(); i < Math.min(tracks.size(), getScrollY() + tracksInView); ++i) {
			final Track track = tracks.get(i);
			for (int j = getScrollX(); j < Math.min(track.notes.size(), getScrollX() + notesInView); ++j) {
				final Note note = track.notes.get(j);
				note.drawText(gui, i - getScrollY(), j - getScrollX());
			}
		}
	}

	@Override
	public boolean hasSlots() {
		return false;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public void activatedByRail(final int x, final int y, final int z, final boolean active) {
		if (active && !isPlaying()) {
			setPlaying(true);
		}
	}

	private int getTickDelay() {
		switch (speedSetting) {
			case 6: {
				return 1;
			}
			case 5: {
				return 2;
			}
			case 4: {
				return 3;
			}
			case 3: {
				return 5;
			}
			case 2: {
				return 7;
			}
			case 1: {
				return 11;
			}
			case 0: {
				return 13;
			}
			default: {
				return 0;
			}
		}
	}

	@Override
	public void update() {
		super.update();
		if (getCart().world.isRemote) {
			tooLongTrack = false;
			veryLongTrack = false;
			for (int i = 0; i < tracks.size(); ++i) {
				final Track track = tracks.get(i);
				if (track.notes.size() > notesInView) {
					tooLongTrack = true;
					if (track.notes.size() > veryLongTrackLimit) {
						veryLongTrack = true;
					}
				}
				int trackPacketID = -1;
				if (track.addButton.down) {
					track.addButton.down = false;
					trackPacketID = 0;
				} else if (track.removeButton.down) {
					track.removeButton.down = false;
					trackPacketID = 1;
				} else if (track.volumeButton.down) {
					track.volumeButton.down = false;
					trackPacketID = 2;
				}
				if (trackPacketID != -1) {
					final byte info = (byte) (i | trackPacketID << 4);
					sendPacket(1, info);
				}
			}
			if (!tooLongTrack) {
				pixelScrollX = 0;
				isScrollingX = false;
			}
			if (!veryLongTrack) {
				pixelScrollXTune = 0;
				isScrollingXTune = false;
			}
			if (!(tooTallModule = (tracks.size() > tracksInView))) {
				pixelScrollY = 0;
				isScrollingY = false;
			}
			generateScrollX();
			generateScrollY();
			if (createTrack.down) {
				createTrack.down = false;
				sendPacket(0, (byte) 0);
			}
			if (removeTrack.down) {
				removeTrack.down = false;
				sendPacket(0, (byte) 1);
			}
			if (speedButton.down) {
				speedButton.down = false;
				sendPacket(0, (byte) 2);
			}
			for (int i = 0; i < instrumentbuttons.size(); ++i) {
				if (instrumentbuttons.get(i).down && i != currentInstrument) {
					currentInstrument = i;
					break;
				}
			}
			for (int i = 0; i < instrumentbuttons.size(); ++i) {
				if (instrumentbuttons.get(i).down && i != currentInstrument) {
					instrumentbuttons.get(i).down = false;
				}
			}
			if (currentInstrument != -1 && !instrumentbuttons.get(currentInstrument).down) {
				currentInstrument = -1;
			}
		}
		if (isPlaying()) {
			if (currentTick <= 0) {
				boolean found = false;
				for (final Track track2 : tracks) {
					if (track2.notes.size() > playProgress) {
						final Note note = track2.notes.get(playProgress);
						float volume = 0.0f;
						switch (track2.volume) {
							case 0: {
								volume = 0.0f;
								break;
							}
							case 1: {
								volume = 0.33f;
								break;
							}
							case 2: {
								volume = 0.67f;
								break;
							}
							default: {
								volume = 1.0f;
								break;
							}
						}
						note.play(volume);
						found = true;
					}
				}
				if (!found) {
					if (!getCart().world.isRemote) {
						setPlaying(false);
					}
					playProgress = 0;
				} else {
					++playProgress;
				}
				currentTick = getTickDelay() - 1;
			} else {
				--currentTick;
			}
		}
	}

	@Override
	public int guiWidth() {
		return 310;
	}

	@Override
	public int guiHeight() {
		return 150;
	}

	@Override
	public void drawBackground(final GuiMinecart gui, final int x, final int y) {
		ResourceHelper.bindResource("/gui/note.png");
		for (int i = getScrollY(); i < Math.min(tracks.size(), getScrollY() + tracksInView); ++i) {
			final Track track = tracks.get(i);
			for (int j = getScrollX(); j < Math.min(track.notes.size(), getScrollX() + notesInView); ++j) {
				final Note note = track.notes.get(j);
				note.draw(gui, x, y, i - getScrollY(), j - getScrollX());
			}
		}
		for (final Button button : buttons) {
			button.draw(gui, x, y);
		}
		if (tooLongTrack) {
			drawImage(gui, scrollXrect, 48, 0);
			int[] marker = getMarkerX();
			drawImage(gui, marker, 148, 1);
			if (veryLongTrack) {
				marker = getMarkerXTune();
				drawImage(gui, marker, 153, 1);
			}
		} else {
			drawImage(gui, scrollXrect, 48, 16);
		}
		if (tooTallModule) {
			drawImage(gui, scrollYrect, 0, 48);
			final int[] marker = getMarkerY();
			drawImage(gui, marker, 1, 148);
		} else {
			drawImage(gui, scrollYrect, 16, 48);
		}
	}

	private int[] getMarkerX() {
		return generateMarkerX(pixelScrollX);
	}

	private int[] getMarkerXTune() {
		return generateMarkerX(pixelScrollXTune);
	}

	private int[] generateMarkerX(final int x) {
		return new int[] { scrollXrect[0] + x, scrollXrect[1] + 1, 5, 14 };
	}

	private void setMarkerX(final int x) {
		pixelScrollX = generateNewMarkerX(x);
	}

	private void setMarkerXTune(final int x) {
		pixelScrollXTune = generateNewMarkerX(x);
	}

	private int generateNewMarkerX(final int x) {
		int temp = x - scrollXrect[0];
		if (temp < 0) {
			temp = 0;
		} else if (temp > scrollXrect[2] - 5) {
			temp = scrollXrect[2] - 5;
		}
		return temp;
	}

	private int getScrollX() {
		return generatedScrollX;
	}

	private void generateScrollX() {
		if (tooLongTrack) {
			int maxNotes = -1;
			for (int i = 0; i < tracks.size(); ++i) {
				maxNotes = Math.max(maxNotes, tracks.get(i).notes.size());
			}
			maxNotes -= notesInView;
			final float widthOfBlockInScrollArea = (scrollXrect[2] - 5) / maxNotes;
			generatedScrollX = Math.round(pixelScrollX / widthOfBlockInScrollArea);
			if (veryLongTrack) {
				generatedScrollX += (int) (pixelScrollXTune / (scrollXrect[2] - 5) * 50.0f);
			}
		} else {
			generatedScrollX = 0;
		}
	}

	private int[] getMarkerY() {
		return new int[] { scrollYrect[0] + 1, scrollYrect[1] + pixelScrollY, 14, 5 };
	}

	private void setMarkerY(final int y) {
		pixelScrollY = y - scrollYrect[1];
		if (pixelScrollY < 0) {
			pixelScrollY = 0;
		} else if (pixelScrollY > scrollYrect[3] - 5) {
			pixelScrollY = scrollYrect[3] - 5;
		}
	}

	private int getScrollY() {
		return generatedScrollY;
	}

	private void generateScrollY() {
		if (tooTallModule) {
			final int maxTracks = tracks.size() - tracksInView;
			final float heightOfBlockInScrollArea = (scrollYrect[3] - 5) / maxTracks;
			generatedScrollY = Math.round(pixelScrollY / heightOfBlockInScrollArea);
		} else {
			generatedScrollY = 0;
		}
	}

	@Override
	public void drawMouseOver(final GuiMinecart gui, final int x, final int y) {
		for (int i = getScrollY(); i < Math.min(tracks.size(), getScrollY() + tracksInView); ++i) {
			final Track track = tracks.get(i);
			for (int j = getScrollX(); j < Math.min(track.notes.size(), getScrollX() + notesInView); ++j) {
				final Note note = track.notes.get(j);
				if (note.instrumentId != 0) {
					drawStringOnMouseOver(gui, note.toString(), x, y, note.getBounds(i - getScrollY(), j - getScrollX()));
				}
			}
		}
		for (final Button button : buttons) {
			if (button.text != null && button.text.length() > 0) {
				button.overlay(gui, x, y);
			}
		}
	}

	@Override
	public void mouseMovedOrUp(final GuiMinecart gui, final int x, final int y, final int button) {
		if (isScrollingX) {
			setMarkerX(x);
			if (button != -1) {
				isScrollingX = false;
			}
		}
		if (isScrollingXTune) {
			setMarkerXTune(x);
			if (button != -1) {
				isScrollingXTune = false;
			}
		}
		if (isScrollingY) {
			setMarkerY(y + getCart().getRealScrollY());
			if (button != -1) {
				isScrollingY = false;
			}
		}
	}

	@Override
	public void mouseClicked(final GuiMinecart gui, final int x, final int y, final int buttonId) {
		if (buttonId == 0) {
			for (final Button button : buttons) {
				button.clicked(x, y);
			}
			if (!isScrollingX && inRect(x, y, scrollXrect)) {
				isScrollingX = true;
			} else if (!isScrollingY && inRect(x, y, scrollYrect)) {
				isScrollingY = true;
			}
		} else if (buttonId == 1 && !isScrollingXTune && inRect(x, y, scrollXrect)) {
			isScrollingXTune = true;
		}
		if (buttonId == 0 || buttonId == 1) {
			for (int i = getScrollY(); i < Math.min(tracks.size(), getScrollY() + tracksInView); ++i) {
				final Track track = tracks.get(i);
				for (int j = getScrollX(); j < Math.min(track.notes.size(), getScrollX() + notesInView); ++j) {
					final Note note = track.notes.get(j);
					if (inRect(x, y, note.getBounds(i - getScrollY(), j - getScrollX()))) {
						int instrumentInfo = currentInstrument;
						if (instrumentInfo == -1) {
							if (buttonId == 0) {
								instrumentInfo = 6;
							} else {
								instrumentInfo = 7;
							}
						}
						if (currentInstrument != -1 || note.instrumentId != 0) {
							byte info = (byte) i;
							info |= (byte) (instrumentInfo << 4);
							sendPacket(2, new byte[] { info, (byte) j });
						}
					}
				}
			}
		}
	}

	@Override
	public int numberOfGuiData() {
		return 1 + (maximumNotesPerTrack + 1) * maximumTracksPerModule;
	}

	@Override
	protected void checkGuiData(final Object[] info) {
		short moduleHeader = (short) tracks.size();
		moduleHeader |= (short) (speedSetting << 4);
		updateGuiData(info, 0, moduleHeader);
		for (int i = 0; i < tracks.size(); ++i) {
			final Track track = tracks.get(i);
			updateGuiData(info, 1 + (maximumNotesPerTrack + 1) * i, track.getInfo());
			for (int j = 0; j < track.notes.size(); ++j) {
				final Note note = track.notes.get(j);
				updateGuiData(info, 1 + (maximumNotesPerTrack + 1) * i + 1 + j, note.getInfo());
			}
		}
	}

	@Override
	public void receiveGuiData(int id, final short data) {
		if (id == 0) {
			final int trackCount = data & maximumTracksPerModule;
			speedSetting = (data & ~maximumTracksPerModule) >> 4;
			updateSpeedButton();
			while (tracks.size() < trackCount) {
				new Track();
			}
			while (tracks.size() > trackCount) {
				tracks.get(tracks.size() - 1).unload();
				tracks.remove(tracks.size() - 1);
			}
		} else {
			final int trackId = --id / (maximumNotesPerTrack + 1);
			int noteId = id % (maximumNotesPerTrack + 1);
			final Track track = tracks.get(trackId);
			if (noteId == 0) {
				track.setInfo(data);
			} else {
				--noteId;
				final Note note = track.notes.get(noteId);
				note.setInfo(data);
			}
		}
	}

	@Override
	public int numberOfDataWatchers() {
		return 1;
	}

	@Override
	public void initDw() {
		PLAYING = createDw(DataSerializers.BOOLEAN);
		registerDw(PLAYING, false);
	}

	private boolean isPlaying() {
		return !isPlaceholder() && (getDw(PLAYING) || playProgress > 0);
	}

	private void setPlaying(final boolean val) {
		updateDw(PLAYING, val);
	}

	@Override
	public int numberOfPackets() {
		return 3;
	}

	@Override
	protected void receivePacket(final int id, final byte[] data, final EntityPlayer player) {
		if (id == 0) {
			if (data[0] == 0) {
				if (tracks.size() < maximumTracksPerModule) {
					new Track();
				}
			} else if (data[0] == 1) {
				if (tracks.size() > 0) {
					tracks.remove(tracks.size() - 1);
				}
			} else if (data[0] == 2) {
				++speedSetting;
				if (speedSetting >= 7) {
					speedSetting = 0;
				}
			}
		} else if (id == 1) {
			final int trackID = data[0] & maximumTracksPerModule;
			final int trackPacketID = (data[0] & ~maximumTracksPerModule) >> 4;
			if (trackID < tracks.size()) {
				final Track track = tracks.get(trackID);
				if (trackPacketID == 0) {
					if (track.notes.size() < maximumNotesPerTrack) {
						new Note(track);
					}
				} else if (trackPacketID == 1) {
					if (track.notes.size() > 0) {
						track.notes.remove(track.notes.size() - 1);
					}
				} else if (trackPacketID == 2) {
					track.volume = (track.volume + 1) % 4;
				}
			}
		} else if (id == 2) {
			final byte info = data[0];
			final byte noteID = data[1];
			final byte trackID2 = (byte) (info & maximumTracksPerModule);
			final byte instrumentInfo = (byte) ((byte) (info & ~(byte) maximumTracksPerModule) >> 4);
			if (trackID2 < tracks.size()) {
				final Track track2 = tracks.get(trackID2);
				if (noteID < track2.notes.size()) {
					final Note note = track2.notes.get(noteID);
					if (instrumentInfo < 6) {
						note.instrumentId = instrumentInfo;
					} else if (instrumentInfo == 6) {
						final Note note2 = note;
						++note2.pitch;
						if (note.pitch > 24) {
							note.pitch = 0;
						}
					} else {
						final Note note3 = note;
						--note3.pitch;
						if (note.pitch < 0) {
							note.pitch = 24;
						}
					}
				}
			}
		}
	}

	@Override
	protected void Save(final NBTTagCompound tagCompound, final int id) {
		short headerInfo = (short) tracks.size();
		headerInfo |= (short) (speedSetting << 4);
		tagCompound.setShort(generateNBTName("Header", id), headerInfo);
		for (int i = 0; i < tracks.size(); ++i) {
			final Track track = tracks.get(i);
			tagCompound.setShort(generateNBTName("Track" + i, id), track.getInfo());
			for (int j = 0; j < track.notes.size(); ++j) {
				final Note note = track.notes.get(j);
				tagCompound.setShort(generateNBTName("Note" + i + ":" + j, id), note.getInfo());
			}
		}
	}

	@Override
	protected void Load(final NBTTagCompound tagCompound, final int id) {
		final short headerInfo = tagCompound.getShort(generateNBTName("Header", id));
		receiveGuiData(0, headerInfo);
		for (int i = 0; i < tracks.size(); ++i) {
			final short trackInfo = tagCompound.getShort(generateNBTName("Track" + i, id));
			receiveGuiData(1 + (maximumNotesPerTrack + 1) * i, trackInfo);
			final Track track = tracks.get(i);
			for (int j = 0; j < track.notes.size(); ++j) {
				final short noteInfo = tagCompound.getShort(generateNBTName("Note" + i + ":" + j, id));
				receiveGuiData(1 + (maximumNotesPerTrack + 1) * i + 1 + j, noteInfo);
			}
		}
	}

	private class TrackButton extends Button {
		private int trackID;
		private int x;

		public TrackButton(final int x, final int trackID) {
			super(0, 0);
			this.trackID = trackID;
			this.x = x;
		}

		@Override
		public int[] getRect() {
			return new int[] { x, notemapY + (trackID - getScrollY()) * trackHeight, 16, 16 };
		}

		private boolean isValid() {
			return getScrollY() <= trackID && trackID < getScrollY() + tracksInView;
		}

		@Override
		public void draw(final GuiMinecart gui, final int x, final int y) {
			if (isValid()) {
				super.draw(gui, x, y);
			}
		}

		@Override
		public void overlay(final GuiMinecart gui, final int x, final int y) {
			if (isValid()) {
				super.overlay(gui, x, y);
			}
		}

		@Override
		public void clicked(final int x, final int y) {
			if (isValid()) {
				super.clicked(x, y);
			}
		}
	}

	private class Button {
		public int[] rect;
		public boolean down;
		public String text;
		public int color;
		public int imageID;

		public Button(final int x, final int y) {
			down = false;
			rect = new int[] { x, y, 16, 16 };
			color = 0;
			imageID = -1;
			buttons.add(this);
		}

		public int[] getRect() {
			return rect;
		}

		public void overlay(final GuiMinecart gui, final int x, final int y) {
			drawStringOnMouseOver(gui, text, x, y, getRect());
		}

		public void clicked(final int x, final int y) {
			if (inRect(x, y, getRect())) {
				down = !down;
			}
		}

		public void draw(final GuiMinecart gui, final int x, final int y) {
			if (!inRect(x, y, getRect())) {
				GL11.glColor4f((color >> 16) / 255.0f, (color >> 8 & 0xFF) / 255.0f, (color & 0xFF) / 255.0f, 1.0f);
			}
			drawImage(gui, getRect(), 32, 0);
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			int srcX = 0;
			final int srcY = 16;
			if (down) {
				srcX += 16;
			}
			drawImage(gui, getRect(), srcX, srcY);
			if (imageID != -1) {
				drawImage(gui, getRect(), imageID * 16, 32);
			}
		}
	}

	private class Note {
		public int instrumentId;
		public int pitch;

		public Note(final Track track) {
			track.notes.add(this);
		}

		public void drawText(final GuiMinecart gui, final int trackID, final int noteID) {
			if (instrumentId == 0) {
				return;
			}
			final int[] rect = getBounds(trackID, noteID);
			String str = String.valueOf(pitch);
			if (str.length() < 2) {
				str = "0" + str;
			}
			drawString(gui, str, rect[0] + 3, rect[1] + 6, instrumentColors[instrumentId]);
		}

		public void draw(final GuiMinecart gui, final int x, final int y, final int trackID, final int noteID) {
			int srcX = 0;
			if (instrumentId == 0) {
				srcX += 16;
			}
			final int[] rect = getBounds(trackID, noteID);
			if (instrumentId != 0 && playProgress == noteID + getScrollX() && isPlaying()) {
				GL11.glColor4f(0.3f, 0.3f, 0.3f, 1.0f);
			}
			drawImage(gui, rect, srcX, 0);
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			if (inRect(x, y, rect)) {
				drawImage(gui, rect, 32, 0);
			}
		}

		public int[] getBounds(final int trackID, final int noteID) {
			return new int[] { notemapX + noteID * 16, notemapY + trackID * trackHeight, 16, 16 };
		}

		public short getInfo() {
			short info = 0;
			info |= (short) instrumentId;
			info |= (short) (pitch << 3);
			return info;
		}

		public void setInfo(final short val) {
			instrumentId = (val & 0x7);
			pitch = (val & 0xF8) >> 3;
		}

		public void play(final float volume) {
			if (instrumentId == 0) {
				return;
			}
			if (!getCart().world.isRemote) {
				if (volume > 0.0f) {
					final float calculatedPitch = (float) Math.pow(2.0, (pitch - 12) / 12.0);
					SoundEvent event = SoundEvents.BLOCK_NOTE_HARP;
					if (instrumentId == 2) {
						event = SoundEvents.BLOCK_NOTE_BASEDRUM;
					} else if (instrumentId == 3) {
						event = SoundEvents.BLOCK_NOTE_SNARE;
					} else if (instrumentId == 4) {
						event = SoundEvents.BLOCK_NOTE_HAT;
					} else if (instrumentId == 5) {
						event = SoundEvents.BLOCK_NOTE_BASS;
					}
					getCart().world.playSound(null, getCart().getPosition(), event, SoundCategory.RECORDS, volume, calculatedPitch);
				}
			} else {
				double oX = 0.0;
				double oZ = 0.0;
				if (getCart().motionX != 0.0) {
					oX = ((getCart().motionX > 0.0) ? -1 : 1);
				}
				if (getCart().motionZ != 0.0) {
					oZ = ((getCart().motionZ > 0.0) ? -1 : 1);
				}
				getCart().world.spawnParticle(EnumParticleTypes.NOTE, getCart().x() + oZ * 1.0 + 0.5, getCart().y() + 1.2, getCart().z() + oX * 1.0 + 0.5, pitch / 24.0, 0.0, 0.0);
				getCart().world.spawnParticle(EnumParticleTypes.NOTE, getCart().x() + oZ * -1.0 + 0.5, getCart().y() + 1.2, getCart().z() + oX * -1.0 + 0.5, pitch / 24.0, 0.0, 0.0);
			}
		}

		@Override
		public String toString() {
			if (instrumentId == 0) {
				return "Unknown instrument";
			}
			return instrumentNames[instrumentId - 1].translate() + " " + pitchNames[pitch];
		}
	}

	private class Track {
		public ArrayList<Note> notes;
		public Button addButton;
		public Button removeButton;
		public Button volumeButton;
		public int volume;
		public int lastNoteCount;

		public Track() {
			notes = new ArrayList<>();
			volume = 3;
			if (getCart().world.isRemote) {
				final int ID = tracks.size() + 1;
				addButton = new TrackButton(notemapX - 60, ID - 1);
				addButton.text = Localization.MODULES.ATTACHMENTS.ADD_NOTE.translate(String.valueOf(ID));
				addButton.imageID = 2;
				removeButton = new TrackButton(notemapX - 40, ID - 1);
				removeButton.text = Localization.MODULES.ATTACHMENTS.REMOVE_NOTE.translate(String.valueOf(ID));
				removeButton.imageID = 3;
				volumeButton = new TrackButton(notemapX - 20, ID - 1);
				volumeButton.text = getVolumeText();
				volumeButton.imageID = 4;
			}
			tracks.add(this);
		}

		private String getVolumeText() {
			return Localization.MODULES.ATTACHMENTS.VOLUME.translate(String.valueOf(volume));
		}

		public void unload() {
			buttons.remove(addButton);
			buttons.remove(removeButton);
			buttons.remove(volumeButton);
		}

		public short getInfo() {
			short info = 0;
			info |= (short) notes.size();
			info |= (short) (volume << 12);
			return info;
		}

		public void setInfo(final short val) {
			final int numberofNotes = val & maximumNotesPerTrack;
			while (notes.size() < numberofNotes) {
				new Note(this);
			}
			while (notes.size() > numberofNotes) {
				notes.remove(notes.size() - 1);
			}
			volume = (val & ~maximumNotesPerTrack) >> 12;
			if (getCart().world.isRemote) {
				volumeButton.imageID = 4 + volume;
				volumeButton.text = getVolumeText();
			}
		}
	}
}

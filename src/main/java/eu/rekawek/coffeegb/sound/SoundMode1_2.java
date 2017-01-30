package eu.rekawek.coffeegb.sound;

import static eu.rekawek.coffeegb.Gameboy.TICKS_PER_SEC;

public class SoundMode1_2 extends AbstractSoundMode {

    private final int mode;

    private int freqDivider;

    private int lastOutput;

    private int i;

    private FrequencySweep frequencySweep;

    private VolumeEnvelope volumeEnvelope;

    public SoundMode1_2(int mode) {
        super(mode == 1 ? 0xff10 : 0xff15);
        if (mode != 1 && mode != 2) {
            throw new IllegalArgumentException();
        }
        this.mode = mode;
        this.frequencySweep = new FrequencySweep(nr0, getFrequency());
        this.volumeEnvelope = new VolumeEnvelope(nr2);
    }

    @Override
    public void trigger() {
        if (length.isDisabled()) {
            length.setLength(64);
        }
        this.i = 0;
        resetFreqDivider();
        volumeEnvelope.start();
        frequencySweep.start();
    }

    @Override
    public int tick() {
        if (!dacEnabled) {
            return 0;
        }
        if (!updateLength()) {
            return 0;
        }

        volumeEnvelope.tick();
        frequencySweep.tick();

        if (freqDivider-- == 0) {
            resetFreqDivider();
            lastOutput = ((getDuty() & (1 << i)) >> i);
            lastOutput *= volumeEnvelope.getVolume();
            i = (i + 1) % 8;
        }
        return lastOutput;
    }

    @Override
    protected void setNr0(int value) {
        super.setNr0(value);
        if (mode == 1) {
            frequencySweep = new FrequencySweep(value, getFrequency());
        }
    }

    @Override
    protected void setNr1(int value) {
        super.setNr1(value);
        length.setLength(64 - (value & 0b00111111));
    }

    @Override
    protected void setNr2(int value) {
        super.setNr2(value);
        volumeEnvelope = new VolumeEnvelope(value);
        dacEnabled = (value & 0b11111000) != 0;
        enabled &= dacEnabled;
    }

    private int getDuty() {
        switch (getNr1() >> 6) {
            case 0:
                return 0b00000001;
            case 1:
                return 0b10000001;
            case 2:
                return 0b10000111;
            case 3:
                return 0b01111110;
            default:
                throw new IllegalStateException();
        }
    }

    private void resetFreqDivider() {
        if (frequencySweep.isEnabled()) {
            freqDivider = frequencySweep.getFreq() * 4;
        } else {
            freqDivider = getFrequency() * 4;
        }
    }
}

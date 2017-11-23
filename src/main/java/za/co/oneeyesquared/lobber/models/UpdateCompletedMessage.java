package za.co.oneeyesquared.lobber.models;

/**
 * Created by Balitha on 2017-11-23.
 */
public class UpdateCompletedMessage {
    private long sequenceID;

    public UpdateCompletedMessage(long sequenceID) {
        this.sequenceID = sequenceID;
    }

    public long getSequenceID() {
        return sequenceID;
    }

    public void setSequenceID(long sequenceID) {
        this.sequenceID = sequenceID;
    }
}

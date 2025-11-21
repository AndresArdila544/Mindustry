package mindustry.server.setupItems;

public abstract class SetupItem {

    public void setup() {

    }

    public abstract void initiateEvents();

    // non-event related setup steps
    public abstract void setupSteps();

}

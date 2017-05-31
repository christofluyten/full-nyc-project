package fileMaker;

import java.io.IOException;

/**
 * Created by christof on 18.11.16.
 */

/*Deze functie neemt een New York City Taxi Data file, een start- en eindtijd, een kaart en een
 positie->wegen-map als input en produceert een .CSV-file met pickuptimes, ophaal- en afzetlocaties .*/

public class SimulationObjectHandler {


    private IOHandler ioHandler;


    public SimulationObjectHandler(IOHandler ioHandler) throws IOException, ClassNotFoundException {
        this.ioHandler = ioHandler;
//        positionToClosestLinksMap = Link.getPositionToClosestLinksMap(ptclFileName);
    }

    public void extractAndPositionPassengers() throws IOException, ClassNotFoundException {
        Extractor extractor = new Extractor(getIoHandler());
        extractor.extractAndPositionPassengers();
    }

    public void extractAndPositionTaxis() throws IOException, ClassNotFoundException {
        Extractor extractor= new Extractor(getIoHandler());
        extractor.extractAndPositionTaxis();
    }

    public IOHandler getIoHandler() {
        return ioHandler;
    }
}

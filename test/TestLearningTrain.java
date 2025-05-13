import othello.gamelogic.*;
import othello.gamelogic.LearningTrain;
import othello.gamelogic.TDLearningStrategy;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.junit.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.deeplearning4j.util.ModelSerializer;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TestLearningTrain {

    private LearningTrain lt;

    @Before
    public void init() {                // fresh instance for every test
        lt = new LearningTrain();
    }

    /* ==================================================================== */
    /* saveTrainedModel / loadTrainedModel                                  */
    /* ==================================================================== */

    @Test
    public void saveTrainedModelNoModelTest() throws Exception {
        // should print a warning when no model is set
        String msg = tapSystemOut(() -> lt.saveTrainedModel("x.zip"));
        assertTrue(msg.contains("There is no model"));
    }

    @Test
    public void loadTrainedModelMissingFileTest() throws Exception {
        // loading a non-existent file should warn
        File tmp = File.createTempFile("ghost", ".zip");
        //noinspection ResultOfMethodCallIgnored
        tmp.delete();
        String msg = tapSystemOut(() ->
                lt.loadTrainedModel(tmp.getAbsolutePath(), .8, .02));
        assertTrue(msg.contains("cannot be found"));
    }

    @Test
    public void saveLoadRoundTripTest() throws Exception {
        // end-to-end save then load
        MultiLayerNetwork tiny = buildTinyNet();

        TDLearningStrategy stub = mock(TDLearningStrategy.class);
        when(stub.getCNNModel()).thenReturn(tiny);

        Field f = LearningTrain.class.getDeclaredField("trained");
        f.setAccessible(true);
        f.set(lt, stub);

        File tmp = File.createTempFile("net", ".zip"); tmp.deleteOnExit();
        tapSystemOut(() -> lt.saveTrainedModel(tmp.getAbsolutePath()));
        assertTrue(tmp.length() > 0);

        LearningTrain again = new LearningTrain();
        tapSystemOut(() -> again.loadTrainedModel(tmp.getAbsolutePath(), .7, .03));

        Field f2 = LearningTrain.class.getDeclaredField("trained");
        f2.setAccessible(true);
        assertNotNull(f2.get(again));
    }

    /* ==================================================================== */
    /* repeatGame → hits singleGame internally                               */
    /* ==================================================================== */

    @Test
    public void repeatGameOneRoundTest() throws Exception {
        // one round is enough to exercise the while-loop
        tapSystemOut(() -> lt.repeatGame(1));
    }

    /* ==================================================================== */
    /* trainVsMinimax / noTrainingCustomVsMinimax                           */
    /* ==================================================================== */

    /* ==================================================================== */
    /* getTrainedModel / getCopy                                            */
    /* ==================================================================== */

//    @Test
//    public void getTrainedModelMissingFileTest() throws Exception {
//        // path hard-coded inside method – expect null + console message
//        String console = tapSystemOut(() -> assertNull(lt.getTrainedModel()));
//        assertTrue(console.contains("cannot be found")
//                || console.contains("Model has been trained!"));
//    }

    @Test
    public void getCopyDeepCopyTest() throws Exception {
        // ensure getCopy produces a deep copy of the board
        BoardSpace[][] origin = new BoardSpace[1][1];
        origin[0][0] = new BoardSpace(0, 0, BoardSpace.SpaceType.EMPTY);

        Method m = LearningTrain.class.getDeclaredMethod(
                "getCopy", BoardSpace[][].class);
        m.setAccessible(true);
        BoardSpace[][] copy = (BoardSpace[][]) m.invoke(lt, (Object) origin);

        assertNotSame(origin, copy);
        copy[0][0].setType(BoardSpace.SpaceType.BLACK);
        assertNotEquals(origin[0][0].getType(), copy[0][0].getType());
    }

    /* ==================================================================== */
    /* singleGame edge-case: forced draw                                    */
    /* ==================================================================== */

    @Test
    public void singleGameDrawTest() throws Exception {
        // both players have no moves – score should be 0 ± tolerance
        BoardSpace[][] full = new BoardSpace[8][8];
        int idx = 0;
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++) {
                BoardSpace.SpaceType t = (idx++ % 2 == 0) ?
                        BoardSpace.SpaceType.BLACK : BoardSpace.SpaceType.WHITE;
                full[x][y] = new BoardSpace(x, y, t);
            }

        OthelloGame g = mock(OthelloGame.class, RETURNS_DEEP_STUBS);
        when(g.getBoard()).thenReturn(full);
        when(g.getAvailableMoves(any())).thenReturn(null);  // force end

        Method m = LearningTrain.class.getDeclaredMethod(
                "singleGame", ComputerPlayer.class, ComputerPlayer.class);
        m.setAccessible(true);

        ComputerPlayer p1 = new ComputerPlayer("custom"),
                p2 = new ComputerPlayer("custom");
        p1.setColor(BoardSpace.SpaceType.BLACK);
        p2.setColor(BoardSpace.SpaceType.WHITE);

        double err = (double) m.invoke(new LearningTrain(), p1, p2);
        assertEquals(0.0, err, 0.05);                      // allow tiny drift
    }

    /* ==================================================================== */
    /* repeatGame should eventually set trained                             */
    /* ==================================================================== */

    @Test
    public void repeatGameUpdatesTrainedTest() throws Exception {
        // after at least one game, trained should be non-null
        LearningTrain trainer = new LearningTrain();
        tapSystemOut(() -> trainer.repeatGame(1));
        Field fld = LearningTrain.class.getDeclaredField("trained");
        fld.setAccessible(true);
        assertNotNull(fld.get(trainer));
    }

    /* ==================================================================== */
    /* loadTrainedModel extreme γ/α                                         */
    /* ==================================================================== */

    @Test
    public void loadTrainedModelGammaAlphaBoundsTest() throws Exception {
        // γ = 1, α = 0 – common extrema, just ensure no exception
        File tmp = File.createTempFile("tiny", ".zip"); tmp.deleteOnExit();
        ModelSerializer.writeModel(buildTinyNet(), tmp, true);
        tapSystemOut(() ->
                lt.loadTrainedModel(tmp.getAbsolutePath(), 1.0, 0.0));
        Field fld = LearningTrain.class.getDeclaredField("trained");
        fld.setAccessible(true);
        assertNotNull(fld.get(lt));
    }

    /* ==================================================================== */
    /* saveTrainedModel success path                                        */
    /* ==================================================================== */

    @Test
    public void saveTrainedModelSuccessTest() throws Exception {
        // inject a fake model then save – file must be non-empty
        injectFakeModel(lt);
        File f = File.createTempFile("ckpt", ".zip"); f.deleteOnExit();
        tapSystemOut(() -> lt.saveTrainedModel(f.getAbsolutePath()));
        assertTrue(f.length() > 100);
    }

    /* ==================================================================== */
    /* trainVsMinimax with stub strategy                                    */
    /* ==================================================================== */

    @Test
    public void trainVsMinimaxStubModelTest() throws Exception {
        // with non-null trained, method should run even for 0 games
        Field f = LearningTrain.class.getDeclaredField("trained");
        f.setAccessible(true);
        f.set(lt, mock(TDLearningStrategy.class));
        String out = tapSystemOut(() -> lt.trainVsMinimax(0));
        assertTrue(out.contains("wins a total of"));
    }

    /* ==================================================================== */
    /* ---------- helpers -------------------------------------------------- */
    /* ==================================================================== */

    /** build a minimal valid network (2-class softmax) */
    private static MultiLayerNetwork buildTinyNet() {
        MultiLayerConfiguration cfg = new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER).list()
                .layer(new DenseLayer.Builder().nIn(4).nOut(3)
                        .activation(Activation.RELU).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nIn(3).nOut(2)
                        .activation(Activation.SOFTMAX).build())
                .build();
        MultiLayerNetwork net = new MultiLayerNetwork(cfg);
        net.init();
        return net;
    }

    /** injects a fake TDLearningStrategy whose model = buildTinyNet() */
    private static void injectFakeModel(LearningTrain trg) throws Exception {
        TDLearningStrategy fake = mock(TDLearningStrategy.class);
        when(fake.getCNNModel()).thenReturn(buildTinyNet());
        Field fld = LearningTrain.class.getDeclaredField("trained");
        fld.setAccessible(true);
        fld.set(trg, fake);
    }
}
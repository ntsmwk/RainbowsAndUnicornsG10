package at.jku.cp.rau.tests;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import at.jku.cp.rau.game.BitBoard;
import at.jku.cp.rau.game.Board;
import at.jku.cp.rau.game.FasterBoard;
import at.jku.cp.rau.game.IBoard;
import at.jku.cp.rau.game.endconditions.EndCondition;
import at.jku.cp.rau.game.endconditions.NoEnd;
import at.jku.cp.rau.game.objects.Cloud;
import at.jku.cp.rau.game.objects.Marker;
import at.jku.cp.rau.game.objects.Move;
import at.jku.cp.rau.game.objects.Path;
import at.jku.cp.rau.game.objects.Rainbow;
import at.jku.cp.rau.game.objects.Seed;
import at.jku.cp.rau.game.objects.Unicorn;
import at.jku.cp.rau.game.objects.V;

@RunWith(Parameterized.class)
public class TestBoard {
    @Parameters
    public static Collection<Object[]> generateParams() {
        List<Object[]> params = new ArrayList<Object[]>();

        params.add(new Object[] { Board.class });
        params.add(new Object[] { FasterBoard.class });
        params.add(new Object[] { BitBoard.class });

        return params;
    }

    Class<?> clazz;

    public TestBoard(Class<?> clazz) {
        this.clazz = clazz;
    }

    private IBoard fromLevelFile(String filename) {
        try {
            Method m = clazz.getMethod("fromLevelFile", String.class);
            return (IBoard) m.invoke(clazz, Constants.ASSET_PATH + "/default.lvl");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private IBoard fromLevelRepresentation(List<String> lvl) {
        try {
            Method m = clazz.getMethod("fromLevelRepresentation", List.class);
            return (IBoard) m.invoke(clazz, lvl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deepCopyBoard() {

        IBoard a = fromLevelFile(Constants.ASSET_PATH + "/default.lvl");
        IBoard b = a.deepCopy();

        assertEquals(a, b);

        assertNotSame(a.getWalls(), b.getWalls());
        assertNotSame(a.getPaths(), b.getPaths());

        assertNotSame(a.getMarkers(), b.getMarkers());
        assertNotSame(a.getClouds(), b.getClouds());
        assertNotSame(a.getUnicorns(), b.getUnicorns());
        assertNotSame(a.getSeeds(), b.getSeeds());
        assertNotSame(a.getRainbows(), b.getRainbows());
    }

    @Test
    public void copyIsActuallyDifferent() {

        IBoard a = fromLevelFile(Constants.ASSET_PATH + "/default.lvl");
        IBoard b = a.copy();

        assertEquals(a, b);

        b.executeMove(Move.SPAWN);

        assertNotEquals(a, b);
    }

    @Test
    public void copyBoard() {
        IBoard a = fromLevelFile(Constants.ASSET_PATH + "/default.lvl");
        IBoard b = a.copy();

        assertEquals(a, b);

        assertSame(a.getWalls(), b.getWalls());
        assertSame(a.getPaths(), b.getPaths());

        assertNotSame(a.getMarkers(), b.getMarkers());
        assertNotSame(a.getClouds(), b.getClouds());
        assertNotSame(a.getUnicorns(), b.getUnicorns());
        assertNotSame(a.getSeeds(), b.getSeeds());
        assertNotSame(a.getRainbows(), b.getRainbows());
    }

    @Test
    public void seedOnTopOfSeed() {
        IBoard a = fromLevelRepresentation(Arrays.asList("###", "#p#", "###"));

        a.executeMove(Move.SPAWN);
        assertFalse(a.executeMove(Move.SPAWN));
    }

    @Test
    public void seedOnTopOfSeedTwoPlayers() {
        IBoard a = fromLevelRepresentation(Arrays.asList("####", "#pp#", "####"));

        a.executeMove(Move.SPAWN); // p0
        assertFalse(a.executeMove(Move.LEFT)); // p1 cannot go right b/c of
                                               // seed;
    }

    @Test
    public void seedCounter() {
        IBoard a = fromLevelRepresentation(Arrays.asList("###", "#p#", "###"));

        assertTrue(a.executeMove(Move.SPAWN));
        assertEquals(Unicorn.MAX_SEEDS - 1, a.getUnicorns().get(0).seeds);
    }

    @Test
    public void seedCounterResets() {
        IBoard a = fromLevelRepresentation(Arrays.asList("######", "#p...#", "######"));

        assertTrue(a.executeMove(Move.SPAWN));
        assertEquals(Unicorn.MAX_SEEDS - 1, a.getUnicorns().get(0).seeds);

        a.executeMove(Move.RIGHT);
        a.executeMove(Move.RIGHT);
        a.executeMove(Move.RIGHT);

        for (int i = 0; i < Seed.DEFAULT_FUSE - Seed.DEFAULT_RANGE - 1; i++)
            a.executeMove(Move.STAY);

        assertEquals(0, a.getSeeds().size());
        assertEquals(3, a.getRainbows().size());
        assertEquals(Unicorn.MAX_SEEDS, a.getUnicorns().get(0).seeds);
    }

    @Test
    public void noMoreThanMaxSeeds() {
        IBoard a = fromLevelRepresentation(Arrays.asList("#########", "#p......#", "#########"));

        assertTrue(a.executeMove(Move.SPAWN));
        assertTrue(a.executeMove(Move.RIGHT));

        assertTrue(a.executeMove(Move.SPAWN));
        assertTrue(a.executeMove(Move.RIGHT));

        assertThat(a.getPossibleMoves(), hasItem(Move.SPAWN));

        assertTrue(a.executeMove(Move.SPAWN));
        assertTrue(a.executeMove(Move.RIGHT));

        assertEquals(3, a.getSeeds().size());

        assertThat(a.getPossibleMoves(), not(hasItem(Move.SPAWN)));
        assertFalse(a.executeMove(Move.SPAWN));
    }

    @Test
    public void updateSeedsAndRainbows() {
        IBoard a = fromLevelRepresentation(Arrays.asList("###", "#p#", "###"));
        a.setEndCondition(new NoEnd());

        V pos = a.getCurrentUnicorn().pos;

        a.executeMove(Move.SPAWN); // Seed.DEFAULT_FUSE - 1 | 4
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 2 | 3
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 3 | 2
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 4 | 1
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 5 | 0

        for (int i = 0; i < Seed.DEFAULT_FUSE - 5; i++)
            a.executeMove(Move.STAY);

        Rainbow expectedRainbow = new Rainbow(pos, Rainbow.DEFAULT_DURATION - 1);

        assertEquals(1, a.getRainbows().size());
        assertThat(a.at(pos), hasItem(expectedRainbow));
        assertEquals(expectedRainbow, a.getRainbows().get(0));
    }

    class Holder<T> {
        public T obj;

        public Holder() {
            obj = null;
        }
    }

    @Test
    public void updateClouds() {
        List<String> lvl = new ArrayList<String>();

        lvl.add("####");
        lvl.add("#pc#");
        lvl.add("####");

        IBoard a = fromLevelRepresentation(lvl);

        assertEquals(1, a.getClouds().size());

        a.executeMove(Move.SPAWN); // Seed.DEFAULT_FUSE - 1 = 4
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 2 = 3
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 3 = 2
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 4 = 1
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 1 = 0

        for (int i = 0; i < Seed.DEFAULT_FUSE - 5; i++)
            a.executeMove(Move.STAY);

        assertEquals(2, a.getRainbows().size());
        assertEquals(0, a.getClouds().size());
    }

    @Test
    public void rainbowsStopAtClouds() {
        List<String> lvl = new ArrayList<String>();

        lvl.add("#####");
        lvl.add("#pc.#");
        lvl.add("#####");

        IBoard a = fromLevelRepresentation(lvl);

        assertEquals(1, a.getClouds().size());

        a.executeMove(Move.SPAWN); // Seed.DEFAULT_FUSE - 1 = 4
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 2 = 3
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 3 = 2
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 4 = 1
        a.executeMove(Move.STAY); // Seed.DEFAULT_FUSE - 5 = 0

        for (int i = 0; i < Seed.DEFAULT_FUSE - 5; i++)
            a.executeMove(Move.STAY);

        assertEquals(2, a.getRainbows().size());
        assertEquals(0, a.getClouds().size());
    }

    @Test
    public void checkRemovable() {
        List<String> lvl = new ArrayList<String>();

        lvl.add("########");
        lvl.add("#p...cm#");
        lvl.add("########");

        IBoard a = fromLevelRepresentation(lvl);

        assertTrue(a.isRemovable(new V(5, 1)));
        assertTrue(a.executeMove(Move.RIGHT));
        assertTrue(a.executeMove(Move.RIGHT));
        assertTrue(a.executeMove(Move.RIGHT));
        assertFalse(a.executeMove(Move.RIGHT));
        assertTrue(a.executeMove(Move.SPAWN));
        assertTrue(a.executeMove(Move.LEFT));
        assertTrue(a.executeMove(Move.LEFT));
        assertTrue(a.executeMove(Move.LEFT));
    }

    @Test
    public void updateUnicorns() {
        List<String> lvl = new ArrayList<String>();

        lvl.add("######");
        lvl.add("#p..p#");
        lvl.add("######");

        IBoard a = fromLevelRepresentation(lvl);

        // whether the real unicorn is sent sailing, is only testable via a
        // custom
        // endcondition
        final Holder<List<Unicorn>> actualSailing = new Holder<>();

        a.setEndCondition(new EndCondition() {
            @Override
            public boolean hasEnded(IBoard board, List<Cloud> evaporated, List<Unicorn> sailing) {
                if (sailing.size() > 0) {
                    actualSailing.obj = sailing;
                    return true;
                }
                return false;
            }

            @Override
            public int getWinner() {
                return -1;
            }

            @Override
            public EndCondition copy() {
                return this;
            }

            @Override
            public String getOutcome() {
                return "TEST";
            }
        });

        assertEquals(2, a.getUnicorns().size());

        a.executeMove(Move.SPAWN);
        a.executeMove(Move.SPAWN);
        for (int i = 0; i < Seed.DEFAULT_FUSE - 2; i++)
            a.executeMove(Move.STAY);

        assertFalse(a.isRunning());
        assertFalse(a.executeMove(Move.STAY));

        assertEquals(3, a.getRainbows().size());
        assertEquals(1, actualSailing.obj.size());
    }

    @Test
    public void testFileRoundtrip() throws IOException {
        File lvlFile = new File(Files.createTempDirectory(null).toString(), "test.lvl");

        Board expectedBoard = Board.fromLevelRepresentation(Arrays.asList("#######", "#.pcm.#", "#######"));
        expectedBoard.executeMove(Move.LEFT);
        expectedBoard.executeMove(Move.SPAWN);
        expectedBoard.executeMove(Move.RIGHT);
        expectedBoard.executeMove(Move.SPAWN);

        Board.toFile(lvlFile.toString(), expectedBoard);
        IBoard actualBoard = Board.fromFile(lvlFile.toString());

        assertEquals(expectedBoard, actualBoard);
    }

    @Test
    public void whatsAt() {
        IBoard a = fromLevelRepresentation(Arrays.asList("#######", "#.pcm.#", "#######"));
        a.executeMove(Move.SPAWN);

        assertEquals(1, a.at(new V(1, 1)).size()); // path
        assertEquals(3, a.at(new V(2, 1)).size()); // path | unicorn | seed
        assertEquals(2, a.at(new V(3, 1)).size()); // path | cloud
        assertEquals(2, a.at(new V(4, 1)).size()); // path | marker
        assertEquals(1, a.at(new V(5, 1)).size()); // path

        assertEquals(a.at(new V(1, 1)).get(0), new Path(new V(1, 1)));

        assertEquals(a.at(new V(2, 1)).get(0), new Path(new V(2, 1)));
        assertEquals(a.at(new V(2, 1)).get(1), new Unicorn(new V(2, 1), 0));
        assertEquals(a.at(new V(2, 1)).get(2), new Seed(new V(2, 1), 0, Seed.DEFAULT_FUSE - 1, Seed.DEFAULT_RANGE));

        assertEquals(a.at(new V(3, 1)).get(0), new Path(new V(3, 1)));
        assertEquals(a.at(new V(3, 1)).get(1), new Cloud(new V(3, 1)));

        assertEquals(a.at(new V(4, 1)).get(0), new Path(new V(4, 1)));
        assertEquals(a.at(new V(4, 1)).get(1), new Marker(new V(4, 1), -1));

        assertEquals(a.at(new V(5, 1)).get(0), new Path(new V(5, 1)));
    }

    @Test
    public void markersCorrect() {
        IBoard a = fromLevelRepresentation(Arrays.asList("#######", "#..p..#", "#.....#", "#.....#", "#..m..#",
                "#######"));
        assertEquals(1, a.getMarkers().size());
        assertThat(a.getMarkers(), hasItem(new Marker(new V(3, 4))));
    }

    @Test
    public void blankIsNotWalkable() {
        List<String> lines = new ArrayList<>();
        lines.add("#####");
        lines.add("#p m#");
        lines.add("#####");

        IBoard board = fromLevelRepresentation(lines);

        assertFalse(board.isPassable(new V(2, 1)));
    }

    private static byte[] toBytes(IBoard board) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(board);
            out.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static IBoard fromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream ios = new ByteArrayInputStream(bytes);
            ObjectInput in = new ObjectInputStream(ios);
            IBoard board = (IBoard) in.readObject();
            in.close();
            return board;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void isSerializable() throws Exception {
        IBoard board = fromLevelRepresentation(Arrays.asList("#######", "#.pcm.#", "#######"));
        board.getSeeds().add(new Seed(new V(1, 1), 0, 1, 1));
        board.getSeeds().add(new Seed(new V(5, 1), 0, 2, 1));

        assertEquals(board, fromBytes(toBytes(board)));
    }

    @Test
    public void isSerializable2() throws Exception {
        IBoard board = fromLevelRepresentation(Arrays.asList("#######", "#ppppp#", "#######"));
        board.executeMove(Move.LEFT);
        board.executeMove(Move.RIGHT);
        board.executeMove(Move.RIGHT);
        board.executeMove(Move.RIGHT);
        board.executeMove(Move.RIGHT);
        board.executeMove(Move.RIGHT);

        assertEquals(board, fromBytes(toBytes(board)));
    }

    @Test
    public void isSerializable3() throws Exception {
        IBoard board = fromLevelFile(Constants.ASSET_PATH + "/default.lvl");
        board.executeMove(Move.LEFT);
        board.executeMove(Move.RIGHT);
        board.executeMove(Move.DOWN);
        board.executeMove(Move.RIGHT);
        board.executeMove(Move.UP);
        board.executeMove(Move.SPAWN);

        assertEquals(board, fromBytes(toBytes(board)));
    }
}

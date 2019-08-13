package com.sai.javafx.screenruler;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FXScreenRuler extends Application {

    private double DISCRETE = 0.5;
    private long LINE_PAD = 2;
    private long LINE_SIZE = 2 * LINE_PAD + 1;
    private String markerStyle = "-fx-font-size:12px;-fx-font-family:calibri;-fx-fill:#555555;";
    private String fontStyle = "-fx-font-size:15px;-fx-font-family:calibri;";

    private Text windowWidthLbl = new Text();
    private Text windowHeightLbl = new Text();
    private Text lineXLbl = new Text();
    private Text lineYLbl = new Text();
    private Text widthLbl = new Text();
    private Text heightLbl = new Text();
    private Path hMajor = new Path();
    private Path hMinor = new Path();
    private Path vMajor = new Path();
    private Path vMinor = new Path();

    private StackPane root;
    private StackPane hLabelsPane;
    private StackPane vLabelsPane;

    private double lineSceneX, lineSceneY, lineTranX, lineTranY;
    private BooleanProperty rangeLines = new SimpleBooleanProperty();
    private Map<String, Label2D> labelDimensions = new HashMap<>();

    @Override
    public void start(Stage primaryStage) throws Exception {
        initialize();
        final Scene scene = new Scene(buildRoot(), 401, 401, Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.T && e.isControlDown()) {
                rangeLines.set(!rangeLines.get());
            }
        });
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.getIcons().add(new Image(getClass().getResource("ruler.png").toString()));
        primaryStage.show();
        root.addEventHandler(MouseEvent.ANY, new ResizeHandler(primaryStage));
    }

    /**
     * Initializes all the default settings.
     */
    private void initialize() {
        IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000)
                .mapToObj(i -> i + "").forEach(this::computeBounds);
        Stream.of(windowWidthLbl, windowHeightLbl).forEach(lbl -> lbl.setStyle(fontStyle + "-fx-fill:blue;"));
        Stream.of(lineYLbl, lineXLbl).forEach(lbl -> lbl.setStyle(fontStyle + "-fx-font-style:italic;-fx-fill:brown;"));
        Stream.of(widthLbl, heightLbl).forEach(lbl -> lbl.setStyle(fontStyle + "-fx-font-style:italic;-fx-fill:black;"));
        Stream.of(hMajor, hMinor, vMajor, vMinor).forEach(path -> {
            path.setTranslateX(-.5);
            path.setTranslateY(-.5);
            path.setStrokeWidth(1);
            path.setStroke(Color.BLACK);
            path.setStrokeLineCap(StrokeLineCap.BUTT);
        });
    }

    /**
     * Builds the root of the window.
     *
     * @return StackPane as root
     */
    private StackPane buildRoot() {
        root = new StackPane();
        root.setStyle("-fx-background-color:#00000020,#FFFF3320;-fx-background-insets:0,1;");
        root.getChildren().addAll(buildMarkersPane(), new Group(buildDisplayPane()), buildLinesPane());
        root.widthProperty().addListener((obs, old, width) -> {
            windowWidthLbl.setText((width.longValue() - 1) + "px");
            updateHMarkers(width.doubleValue());
        });
        root.heightProperty().addListener((obs, old, height) -> {
            windowHeightLbl.setText((height.longValue() - 1) + "px");
            updateVMarkers(height.doubleValue());
        });
        return root;
    }

    /**
     * Builds the pane that contains all the lines.
     *
     * @return StackPane as container to the lines
     */
    private StackPane buildLinesPane() {
        final StackPane hMainLine = buildMainLine(Orientation.HORIZONTAL);
        final StackPane vMainLine = buildMainLine(Orientation.VERTICAL);
        final StackPane hSubLine = buildSubLine(Orientation.HORIZONTAL, hMainLine);
        final StackPane vSubLine = buildSubLine(Orientation.VERTICAL, vMainLine);

        final StackPane linesPane = new StackPane(hMainLine, vMainLine, hSubLine, vSubLine);
        linesPane.setAlignment(Pos.TOP_LEFT);
        linesPane.maxWidthProperty().bind(root.widthProperty());
        linesPane.maxHeightProperty().bind(root.heightProperty());
        return linesPane;
    }

    /**
     * Builds the pane that hold all the scale labels.
     *
     * @return StackPane as container to all the labels
     */
    private StackPane buildMarkersPane() {
        hLabelsPane = new StackPane();
        hLabelsPane.setAlignment(Pos.TOP_LEFT);
        vLabelsPane = new StackPane();
        vLabelsPane.setAlignment(Pos.TOP_LEFT);
        final StackPane markersPane = new StackPane(hMajor, hMinor, vMajor, vMinor, hLabelsPane, vLabelsPane);
        markersPane.setAlignment(Pos.TOP_LEFT);
        return markersPane;
    }

    /**
     * Builds the pane to display all the measurements. This is actually the end result of the application !!
     *
     * @return GridPane displaying all the labels
     */
    private GridPane buildDisplayPane() {
        final GridPane labelsPane = new GridPane();
        labelsPane.setPadding(new Insets(5));
        labelsPane.setHgap(3);
        labelsPane.setVgap(3);
        labelsPane.setStyle("-fx-background-color:#CCCCCC80;-fx-background-radius:5;");
        labelsPane.addRow(0, text("W:"), windowWidthLbl);
        labelsPane.addRow(1, text("H:"), windowHeightLbl);
        labelsPane.addRow(2, text("X:"), lineXLbl);
        labelsPane.addRow(3, text("Y:"), lineYLbl);
        ColumnConstraints c = new ColumnConstraints();
        c.setHalignment(HPos.CENTER);
        labelsPane.getColumnConstraints().add(c);
        Text widthTxt = text("w:");
        Text heightTxt = text("h:");
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color:grey;");
        rangeLines.addListener((obs, old, show) -> {
            if (show) {
                labelsPane.add(separator, 0, 4, 2, 1);
                labelsPane.addRow(5, widthTxt, widthLbl);
                labelsPane.addRow(6, heightTxt, heightLbl);
            } else {
                labelsPane.getChildren().removeAll(separator, widthTxt, heightTxt, widthLbl, heightLbl);
            }
        });
        return labelsPane;
    }

    private StackPane buildMainLine(Orientation orientation) {
        StackPane lineBox = buildLine(orientation);
        if (orientation == Orientation.HORIZONTAL) {
            lineBox.translateYProperty().addListener((obs, old, y) -> lineYLbl.setText((y.longValue() + LINE_PAD) + "px"));
            lineBox.setTranslateY(100 - LINE_PAD);
            lineBox.setOnMouseDragged(e -> {
                e.consume();
                double offsetY = e.getSceneY() - lineSceneY;
                double buf = lineTranY + offsetY;
                if (buf < 0) {
                    lineBox.setTranslateY(0);
                } else if (buf > root.getHeight() - lineBox.getHeight()) {
                    lineBox.setTranslateY(root.getHeight() - lineBox.getHeight());
                } else {
                    lineBox.setTranslateY(buf);
                }
            });
        } else {
            lineBox.translateXProperty().addListener((obs, old, x) -> {
                System.out.println(x);
                lineXLbl.setText((x.longValue() + LINE_PAD) + "px");
            });
            lineBox.setTranslateX(100 - LINE_PAD);
            lineBox.setOnMouseDragged(e -> {
                e.consume();
                double offsetX = e.getSceneX() - lineSceneX;
                double buf = lineTranX + offsetX;
                if (buf < 0) {
                    lineBox.setTranslateX(0);
                } else if (buf > root.getWidth() - lineBox.getWidth()) {
                    lineBox.setTranslateX(root.getWidth() - lineBox.getWidth());
                } else {
                    lineBox.setTranslateX(buf);
                }
            });
        }
        return lineBox;
    }

    private StackPane buildSubLine(Orientation orientation, StackPane mainLine) {
        StackPane lineBox = buildLine(orientation);
        ((Line) lineBox.getChildren().get(0)).getStrokeDashArray().addAll(10d, 5d);
        lineBox.visibleProperty().bind(rangeLines);
        if (orientation == Orientation.HORIZONTAL) {
            lineBox.translateYProperty().addListener((obs, old, y) -> heightLbl.setText((long) (y.longValue() - mainLine.getTranslateY()) + "px"));
            lineBox.setTranslateY(mainLine.getTranslateY() + 75);
            mainLine.translateYProperty().addListener((obs, old, ty) -> {
                lineBox.setTranslateY(lineBox.getTranslateY() + (ty.doubleValue() - old.doubleValue()));
            });
            lineBox.setOnMouseDragged(e -> {
                e.consume();
                double offsetY = e.getSceneY() - lineSceneY;
                double buf = lineTranY + offsetY;
                if (buf < mainLine.getTranslateY() + LINE_SIZE * 2) {
                    lineBox.setTranslateY(mainLine.getTranslateY() + LINE_SIZE * 2);
                } else if (buf > root.getHeight() - lineBox.getHeight()) {
                    lineBox.setTranslateY(root.getHeight() - lineBox.getHeight());
                } else {
                    lineBox.setTranslateY(buf);
                }
            });
        } else {
            lineBox.translateXProperty().addListener((obs, old, x) -> widthLbl.setText((long) (x.longValue() - mainLine.getTranslateX()) + "px"));
            lineBox.setTranslateX(mainLine.getTranslateX() + 75);
            mainLine.translateXProperty().addListener((obs, old, tx) -> {
                lineBox.setTranslateX(lineBox.getTranslateX() + (tx.doubleValue() - old.doubleValue()));
            });
            lineBox.setOnMouseDragged(e -> {
                e.consume();
                double offsetX = e.getSceneX() - lineSceneX;
                double buf = lineTranX + offsetX;
                if (buf < mainLine.getTranslateX() + LINE_SIZE * 2) {
                    lineBox.setTranslateX(mainLine.getTranslateX() + LINE_SIZE * 2);
                } else if (buf > root.getWidth() - lineBox.getWidth()) {
                    lineBox.setTranslateX(root.getWidth() - lineBox.getWidth());
                } else {
                    lineBox.setTranslateX(buf);
                }
            });
        }
        return lineBox;
    }

    private StackPane buildLine(Orientation orientation) {
        Line line = new Line();
        line.setStrokeWidth(1);
        line.setStroke(Color.RED);
        line.setStartX(0);
        line.setStartY(0);
        line.setEndX(0);
        line.setEndY(0);

        StackPane lineBox = new StackPane(line);
        lineBox.setOnMousePressed(e -> {
            e.consume();
            lineSceneX = e.getSceneX();
            lineSceneY = e.getSceneY();
            lineTranX = lineBox.getTranslateX();
            lineTranY = lineBox.getTranslateY();
        });

        if (orientation == Orientation.HORIZONTAL) {
            lineBox.setCursor(Cursor.V_RESIZE);
            lineBox.setMaxHeight(LINE_SIZE);
            lineBox.setMinHeight(LINE_SIZE);
            line.endXProperty().bind(lineBox.widthProperty());
        } else {
            lineBox.setCursor(Cursor.H_RESIZE);
            lineBox.setMaxWidth(LINE_SIZE);
            lineBox.setMinWidth(LINE_SIZE);
            line.endYProperty().bind(lineBox.heightProperty());
        }
        return lineBox;
    }

    private long getWidth(String text) {
        if (labelDimensions.get(text) == null) {
            computeBounds(text);
        }
        return labelDimensions.get(text).width;
    }

    private long getHeight(String text) {
        if (labelDimensions.get(text) == null) {
            computeBounds(text);
        }
        return labelDimensions.get(text).height;
    }

    private void computeBounds(String key) {
        if (labelDimensions.get(key) == null) {
            final Text text = new Text();
            text.setText(key);
            new Scene(new Group(text));
            text.setStyle(markerStyle);
            text.applyCss();
            Label2D d2d = new Label2D((long) Math.ceil(text.getLayoutBounds().getWidth()),
                    (long) Math.ceil(text.getLayoutBounds().getHeight()));
            labelDimensions.put(key, d2d);
        }
    }

    private Text text(String txt) {
        Text t = new Text(txt);
        t.setStyle(fontStyle + "-fx-font-weight:bold;");
        return t;
    }

    private void updateVMarkers(double height) {
        vLabelsPane.getChildren().clear();
        vMajor.getElements().clear();
        vMinor.getElements().clear();
        double gap = 5;
        for (int i = 0; i < height; i = i + 5) {
            double size = 2 * gap;
            String txt = null;
            if (i != 0 && i % 100 == 0) {
                size = 25 + gap;
                vMajor.getElements().addAll(new MoveTo(0, i));
                txt = i + "";
            } else if (i != 0 && i % 10 == 0) {
                size = 10 + gap;
                vMajor.getElements().addAll(new MoveTo(0, i));
                if (i % 50 == 0) {
                    txt = i + "";
                }
            } else {
                vMajor.getElements().addAll(new MoveTo(gap, i));
            }
            vMajor.getElements().addAll(new HLineTo(size));
            if (txt != null) {
                Text text = new Text(txt);
                text.setStyle(markerStyle);
                text.setTranslateX(size + 2);
                text.setTranslateY(i - (getHeight(txt) / 2) + 1);
                vLabelsPane.getChildren().add(text);
            }
        }

        for (int i = 0; i < height; i = i + 2) {
            if (i == 0 || i % 10 != 0) {
                vMinor.getElements().addAll(new MoveTo(0, i), new HLineTo(gap));
            }
        }
    }

    private void updateHMarkers(double width) {
        hLabelsPane.getChildren().clear();
        hMajor.getElements().clear();
        hMinor.getElements().clear();
        double gap = 5;
        for (int i = 0; i < width; i = i + 5) {
            double size = 2 * gap;
            String txt = null;
            if (i != 0 && i % 100 == 0) {
                size = 25 + gap;
                hMajor.getElements().add(new MoveTo(i, 0));
                txt = i + "";
            } else if (i != 0 && i % 10 == 0) {
                size = 10 + gap;
                hMajor.getElements().add(new MoveTo(i, 0));
                txt = ((i % 100) / 10) + "";
            } else {
                hMajor.getElements().add(new MoveTo(i, gap));
            }
            hMajor.getElements().addAll(new VLineTo(size));
            if (txt != null) {
                Text text = new Text(txt);
                text.setStyle(markerStyle);
                text.setTranslateY(size + 2);
                text.setTranslateX(i - (getWidth(txt) / 2));
                hLabelsPane.getChildren().add(text);
            }
        }
        for (int i = 0; i < width; i = i + 2) {
            if (i == 0 || i % 10 != 0) {
                hMinor.getElements().addAll(new MoveTo(i, 0), new VLineTo(gap));
            }
        }
    }

    class Label2D {
        private long width;
        private long height;

        public Label2D(long width, long height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Handler to process the resizing of the the given stage.
     */
    class ResizeHandler implements EventHandler<MouseEvent> {

        /**
         * Space to consider around the stage border for resizing
         */
        private static final double BORDER = 6;

        /**
         * Stage to which the handler is implemented
         */
        private final Stage stage;

        /**
         * Current cursor reference to the scene
         */
        private Cursor cursor = Cursor.DEFAULT;

        /**
         * X position of the drag start
         */
        private double startX = 0;

        /**
         * Y position of the drag start
         */
        private double startY = 0;

        /**
         * X position of the window move start
         */
        private double moveStartX = 0;

        /**
         * Y position of the window move start
         */
        private double moveStartY = 0;

        /**
         * Constructor.
         *
         * @param stage1 Stage to which resizing to be set.
         */
        ResizeHandler(final Stage stage1) {
            stage = stage1;
        }

        @Override
        public void handle(final MouseEvent event) {
            event.consume();
            final EventType<? extends MouseEvent> eventType = event.getEventType();
            final Scene scene = stage.getScene();
            final double mouseEventX = event.getSceneX();
            final double mouseEventY = event.getSceneY();
            final double sceneWidth = scene.getWidth();
            final double sceneHeight = scene.getHeight();

            if (MouseEvent.MOUSE_MOVED.equals(eventType)) {
                assignCursor(scene, mouseEventX, mouseEventY, sceneWidth, sceneHeight);

            } else if (MouseEvent.MOUSE_PRESSED.equals(eventType)) {
                startX = stage.getWidth() - mouseEventX;
                startY = stage.getHeight() - mouseEventY;
                if (Cursor.DEFAULT.equals(cursor)) {
                    moveStartX = event.getSceneX();
                    moveStartY = event.getSceneY();
                    cursor = Cursor.MOVE;
                    scene.setCursor(cursor);
                }
            } else if (MouseEvent.MOUSE_RELEASED.equals(eventType)) {
                if (Cursor.MOVE.equals(cursor)) {
                    cursor = Cursor.DEFAULT;
                    scene.setCursor(cursor);
                }
            } else if (MouseEvent.MOUSE_DRAGGED.equals(eventType) && !Cursor.DEFAULT.equals(cursor)) {
                if (Cursor.MOVE.equals(cursor)) {
                    handleMove(event);
                } else {
                    if (!Cursor.W_RESIZE.equals(cursor) && !Cursor.E_RESIZE.equals(cursor)) {
                        handleHeightResize(event);
                    }

                    if (!Cursor.N_RESIZE.equals(cursor) && !Cursor.S_RESIZE.equals(cursor)) {
                        handleWidthResize(event);
                    }
                }
            }
        }

        /**
         * Processes moving the window.
         *
         * @param event MouseEvent instance
         */
        private void handleMove(MouseEvent event) {
            final double xOffset = event.getScreenX() - moveStartX;
            final double yOffset = event.getScreenY() - moveStartY;
            stage.setX(xOffset);
            stage.setY(yOffset);
        }


        /**
         * Determines and sets the appropriate cursor based on the mouse position in relative to scene bounds.
         *
         * @param scene       Stage scene
         * @param mouseEventX X position of mouse in the scene
         * @param mouseEventY Y position of mouse in the scene
         * @param sceneWidth  Width of the scene
         * @param sceneHeight Height of the scene
         */
        private void assignCursor(final Scene scene, final double mouseEventX,
                                  final double mouseEventY, final double sceneWidth, final double sceneHeight) {
            final Cursor cursor1;
            if (mouseEventX < BORDER && mouseEventY < BORDER) {
                cursor1 = Cursor.NW_RESIZE;
            } else if (mouseEventX < BORDER && mouseEventY > sceneHeight - BORDER) {
                cursor1 = Cursor.SW_RESIZE;
            } else if (mouseEventX > sceneWidth - BORDER
                    && mouseEventY < BORDER) {
                cursor1 = Cursor.NE_RESIZE;
            } else if (mouseEventX > sceneWidth - BORDER && mouseEventY > sceneHeight - BORDER) {
                cursor1 = Cursor.SE_RESIZE;
            } else if (mouseEventX < BORDER) {
                cursor1 = Cursor.W_RESIZE;
            } else if (mouseEventX > sceneWidth - BORDER) {
                cursor1 = Cursor.E_RESIZE;
            } else if (mouseEventY < BORDER) {
                cursor1 = Cursor.N_RESIZE;
            } else if (mouseEventY > sceneHeight - BORDER) {
                cursor1 = Cursor.S_RESIZE;
            } else {
                cursor1 = Cursor.DEFAULT;
            }
            cursor = cursor1;
            scene.setCursor(cursor);
        }

        /**
         * Processes the vertical drag movement and resizes the window height.
         *
         * @param event MouseEvent instance
         */
        private void handleHeightResize(final MouseEvent event) {
            final double mouseEventY = event.getSceneY();
            double minHeight = 30;
            if (Cursor.NW_RESIZE.equals(cursor)
                    || Cursor.N_RESIZE.equals(cursor)
                    || Cursor.NE_RESIZE.equals(cursor)) {
                if (stage.getHeight() > minHeight || mouseEventY < 0) {
                    final double newHeight = stage.getY() - event.getScreenY() + stage.getHeight();
                    stage.setHeight(max(newHeight, minHeight));
                    stage.setY(event.getScreenY());
                }
            } else if (stage.getHeight() > minHeight || mouseEventY + startY - stage.getHeight() > 0) {
                final double newHeight = mouseEventY + startY;
                stage.setHeight(max(newHeight, minHeight));
            }
        }

        /**
         * Processes the horizontal drag movement and resizes the window width.
         *
         * @param event MouseEvent instance
         */
        private void handleWidthResize(final MouseEvent event) {
            final double mouseEventX = event.getSceneX();
            double minWidth = 30;
            if (Cursor.NW_RESIZE.equals(cursor)
                    || Cursor.W_RESIZE.equals(cursor)
                    || Cursor.SW_RESIZE.equals(cursor)) {
                if (stage.getWidth() > minWidth || mouseEventX < 0) {
                    final double newWidth = stage.getX() - event.getScreenX() + stage.getWidth();
                    stage.setWidth(max(newWidth, minWidth));
                    stage.setX(event.getScreenX());
                }
            } else if (stage.getWidth() > minWidth || mouseEventX + startX - stage.getWidth() > 0) {
                final double newWidth = mouseEventX + startX;
                stage.setWidth(max(newWidth, minWidth));
            }
        }

        /**
         * Determines the max value among the provided two values.
         *
         * @param value1 First value
         * @param value2 Second value
         * @return Maximum value of the given two values.
         */
        private double max(final double value1, final double value2) {
            return value1 > value2 ? value1 : value2;
        }
    }

}

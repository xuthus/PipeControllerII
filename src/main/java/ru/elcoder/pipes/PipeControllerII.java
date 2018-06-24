package ru.elcoder.pipes;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PipeControllerII {

	private static final String TOP_SCORES = "top scores";
	private static final String NEW_GAME = "new game";
	private static final int IMAGE_SIZE = 64;
	private static final int BOARD_SIZE = 10;
	private static final String GAME_CODE = "pipes-" + BOARD_SIZE;
	private static final int CELL_SIZE = IMAGE_SIZE;
	private static final int FRAME_HEIGHT = (CELL_SIZE) * BOARD_SIZE + 70;
	private static final int FRAME_WIDTH = (CELL_SIZE) * BOARD_SIZE + 150;
	private static final String[][] imageCodes = {
			{"****", "X.gif"},
			{"* * ", "SN.gif"},
			{" * *", "WO.gif"},
			{"**  ", "NO.gif"},
			{" ** ", "OS.gif"},
			{"  **", "SW.gif"},
			{"*  *", "WN.gif"},
			{"*** ", "NOS.gif"},
			{"** *", "NOW.gif"},
			{"* **", "NSW.gif"},
			{" ***", "OSW.gif"}
	};
	private static final Map<String, ImageIcon> images = new HashMap<>(imageCodes.length);
	private static final String FILLED_PIPE_SUFFIX = "-on";
	private static final String EMPTY_GLASS = "empty";
	private static final String FULL_GLASS = "full";
	private static final int MAX_SCORES_COUNT = 20;
	private JLabel scoresLabel;
	private JLabel timeLabel;
	private int gameScores;
	private long gameStartedAt = System.currentTimeMillis();
	private String playerName = System.getProperty("user.name");
	private boolean gameStarted = false;
	private Timer timer = new Timer(1000, e -> onTimer());

	private void onTimer() {
		if (gameStarted) {
			timeLabel.setText("Time: " + formatTime((int) ((System.currentTimeMillis() - gameStartedAt)) / 1000));
		}
	}

	public static void main(String[] args) throws IOException {
		PipeControllerII game = new PipeControllerII();
		game.loadImages();
		SwingUtilities.invokeLater(game::createGameBoard);
	}

	private static ImageIcon loadIcon(String resourceName) throws IOException {
		return new ImageIcon(ImageIO.read(PipeControllerII.class.getClassLoader().getResource(resourceName)));
	}

	private static void printDebugData() {
	}

	private void repaintScores(int scores) {
		scoresLabel.setText("Scores: " + scores);
	}

	private void loadImages() throws IOException {
		for (String[] imageCode : imageCodes) {
			final String resourceName = imageCode[1];
			images.put(imageCode[0], loadIcon(resourceName));
			final String onResourceName = resourceName.substring(0, resourceName.indexOf('.')) + FILLED_PIPE_SUFFIX + ".gif";
			images.put(imageCode[0] + FILLED_PIPE_SUFFIX, loadIcon(onResourceName));
		}
		images.put("empty", loadIcon("glass_empty.gif"));
		images.put(FULL_GLASS, loadIcon("glass_full.gif"));
	}

	private void createGameBoard() {
		final JFrame frame = new JFrame(PipeControllerII.class.getSimpleName());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
		final Container contentPane = frame.getContentPane();
		contentPane.setLayout(new GridLayout(0, 1));

		final Board data = new Board(BOARD_SIZE);
		newGame(data);

		final JTable table = new JTable(new PipesGameTableModel(BOARD_SIZE, BOARD_SIZE, data));
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				printDebugData();
				gameStarted = true;
				int row = table.rowAtPoint(e.getPoint());
				int col = table.columnAtPoint(e.getPoint());
				if (row > 0 && row < (BOARD_SIZE - 1) && col >= 0) {
					rotateCell(data, row, col, table, e.getButton() == MouseEvent.BUTTON1);
				}
			}
		});
		initTable(table);

		final Box horizontalBox = Box.createHorizontalBox();
		horizontalBox.add(Box.createRigidArea(new Dimension(8, 8)));
		horizontalBox.add(table);
		horizontalBox.add(Box.createGlue());
		final Box verticalBox = Box.createVerticalBox();
		scoresLabel = new JLabel("Scores: 0");
		verticalBox.add(scoresLabel);
		verticalBox.add(Box.createVerticalStrut(8));
		timeLabel = new JLabel("Time: 0:00");
		verticalBox.add(timeLabel);
		verticalBox.add(Box.createVerticalStrut(8));
		final JButton newGameButton = new JButton(NEW_GAME);
		verticalBox.add(newGameButton);
		verticalBox.add(Box.createVerticalStrut(8));
		final JButton topScoresButton = new JButton(TOP_SCORES);
		verticalBox.add(topScoresButton);
		verticalBox.add(Box.createVerticalGlue());
		horizontalBox.add(verticalBox);
		newGameButton.addActionListener(e -> {
			gameStarted = false;
			if (gameScores > 0) {
				saveScores(frame);
			}
			newGame(data);
			table.setModel(new PipesGameTableModel(BOARD_SIZE, BOARD_SIZE, data));
			initTable(table);
			gameStartedAt = System.currentTimeMillis();
		});
		topScoresButton.addActionListener(e -> showTopScores(frame));
		horizontalBox.add(Box.createGlue());
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(horizontalBox);
		panel.setBorder(BorderFactory.createTitledBorder("Game Board"));
		contentPane.add(panel);

		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		timer.start();
	}

	private static String formatTime(int time) {
		int seconds = time % 60;
		int minutes = (time / 60) % 60;
		int hours = time / 3600;
		return (hours > 0 ? String.valueOf(hours) + ":" : "")
				+ (minutes < 10 ? "0" : "") + String.valueOf(minutes) + ":"
				+ (seconds < 10 ? "0" : "") + String.valueOf(seconds);
	}

	private static long parseTime(String text) {
		final String[] parts = text.substring(text.indexOf(" ")).split(":");
		long result = 0L;
		for (int i = 0; i < parts.length; i++) {
			result = result * 60 + Integer.parseInt(parts[i].trim());
		}
		return result;
	}

	private void saveScores(JFrame parentFrame) {
		long time = parseTime(timeLabel.getText());
		TopScoresController controller = new TopScoresController(GAME_CODE);
		try {
			final List<TopScoresController.ScoreItem> scores = controller.loadScores();
			if (scores.size() < MAX_SCORES_COUNT || gameScores >= scores.get(scores.size() - 1).score) {
				String name = askPlayerName(parentFrame, playerName);
				if (name == null) {
					return;
				}
				playerName = name;
				controller.saveScores(new TopScoresController.ScoreItem(gameScores, playerName, (int) time));
				controller.showScores(parentFrame, controller.loadScores());
			}
		} catch (IOException | XMLStreamException e) {
			e.printStackTrace();
		}
	}

	private String askPlayerName(final Frame parentFrame, final String playerName) {
		String name = JOptionPane.showInputDialog("Your name:", playerName);
		if (name == null) {
			return null;
		}
		if (name.isEmpty() || !name.matches("[A-Za-z0-9]*")) {
			JOptionPane.showMessageDialog(parentFrame, "Name can contains only letters and digits", GAME_CODE, JOptionPane.ERROR_MESSAGE);
			name = askPlayerName(parentFrame, name);
		}
		return name;
	}

	private void showTopScores(JFrame parentFrame) {
		TopScoresController controller = new TopScoresController(GAME_CODE);
		try {
			final List<TopScoresController.ScoreItem> scores = controller.loadScores();
			controller.showScores(parentFrame, scores);
		} catch (IOException | XMLStreamException e) {
			e.printStackTrace();
		}
	}

	private void initTable(JTable table) {
		table.setMinimumSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
		table.setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
		table.setCellSelectionEnabled(false);
		for (int i = 0; i < BOARD_SIZE; i++) {
			table.getColumnModel().getColumn(i).setMinWidth(CELL_SIZE);
			table.getColumnModel().getColumn(i).setPreferredWidth(CELL_SIZE);
			table.getColumnModel().getColumn(i).setMaxWidth(CELL_SIZE);
		}
		table.setRowHeight(CELL_SIZE);
		table.setShowGrid(false);
	}

	private void rotateCell(Board board, int row, int col, JTable table, boolean clockwise) {
		final Cell cell = board.getCell(row, col);
		final String cellValue = cell.pipeIcon;
		if (clockwise) {
			cell.pipeIcon = cellValue.substring(3) + cellValue.substring(0, 3);
		} else {
			cell.pipeIcon = cellValue.substring(1) + cellValue.substring(0, 1);
		}
		gameScores = scoreGame(board);
		updateBoard(table, gameScores);
	}

	private void updateBoard(JTable table, int scores) {
		table.repaint();
		repaintScores(scores);
	}

	private void newGame(Board board) {
		final Random random = new Random();
		do {
			for (int column = 0; column < BOARD_SIZE; column++) { // внутри цикла, так как может меняться в scoreGame()
				board.setCell(0, column, new Cell(FULL_GLASS));
				board.setCell(BOARD_SIZE - 1, column, new Cell(EMPTY_GLASS));
			}
			for (int row = 1; row < (BOARD_SIZE - 1); row++) {
				for (int col = 0; col < BOARD_SIZE; col++) {
					board.setCell(row, col, new Cell(imageCodes[random.nextInt(imageCodes.length)][0]));
					// TODO: 02.06.2018 make level strength using non-linear random.nextInt()
				}
			}
		} while (scoreGame(board) != 0);
	}

	/**
	 * вычисляет количество очков
	 *
	 * @param board
	 * @return
	 */
	private int scoreGame(Board board) {
		resetConnections(board);
		// рассчитать наличие у каждой трубы соединений с нижними и верхними источниками
		// если стакан имеет соединение с трубой, имеющей соединение с противоположными стаканами, то стакан меняет свое заполнение
		// количество очков = количество пустых верхних стаканов + количество полных нижних стаканов
		// рекурсией рассчитываем соединения и считаем стаканы

		// цикл по верхним стаканам - проталкиваем соединения во все стороны
		for (int col = 0; col < BOARD_SIZE; col++) {
			if (board.getCell(1, col).isNorth()) {
				applyConnection(board, 1, col, Direction.NORTH);
			}
		}
		// цикл по нижним стаканам
		for (int col = 0; col < BOARD_SIZE; col++) {
			if (board.getCell(BOARD_SIZE - 2, col).isSouth()) {
				applyConnection(board, BOARD_SIZE - 2, col, Direction.SOUTH);
			}
		}
		// считаем стаканы и меняем их иконки
		int scores = 0;
		for (int col = 0; col < BOARD_SIZE; col++) {
			final Cell northGlass = board.getCell(0, col);
			final Boolean southConnected = northGlass.getSouthConnected();
			if (southConnected != null && southConnected) {
				scores++;
				northGlass.pipeIcon = EMPTY_GLASS;
			} else {
				northGlass.pipeIcon = FULL_GLASS;
			}
			final Cell southGlass = board.getCell(BOARD_SIZE - 1, col);
			final Boolean northConnected = southGlass.getConnected(Direction.NORTH);
			if (northConnected != null && northConnected) {
				scores++;
				southGlass.pipeIcon = FULL_GLASS;
			} else {
				southGlass.pipeIcon = EMPTY_GLASS;
			}
		}
		return scores;
	}

	private void applyConnection(Board board, int row, int col, Direction direction) {
		final Cell[][] cells = board.getCells();
		final Cell cell = cells[row][col];
		if (cell.getConnected(direction) != null) {
			return;  // соединение уже проставлено
		}
		cell.setConnected(direction, true);
		int endRow = direction == Direction.SOUTH ? 0 : BOARD_SIZE - 1;
		if (row == endRow) {
			return;  // дошли до противоположного стакана
		}
		if (col > 0 && cell.isWest() && cells[row][col - 1].isEast()) {
			applyConnection(board, row, col - 1, direction);
		}
		if (col < (BOARD_SIZE - 1) && cell.isEast() && cells[row][col + 1].isWest()) {
			applyConnection(board, row, col + 1, direction);
		}
		// вверх, в том числе на стакан
		if (row > 0 && cell.isNorth() && cells[row - 1][col].isSouth()) {
			applyConnection(board, row - 1, col, direction);
		}
		// вниз, в том числе на стакан
		if (row < (BOARD_SIZE - 1) && cell.isSouth() && cells[row + 1][col].isNorth()) {
			applyConnection(board, row + 1, col, direction);
		}
	}

	// сбросить соединения перед расчетом
	private void resetConnections(Board board) {
		for (int row = 0; row < BOARD_SIZE; row++) {
			for (int col = 0; col < BOARD_SIZE; col++) {
				board.getCell(row, col).resetConnections();
			}
		}
		// стаканы всегда имеют связь со своей стороной
		for (int col = 0; col < BOARD_SIZE; col++) {
			board.getCell(0, col).setConnected(Direction.NORTH, true);
			board.getCell(BOARD_SIZE - 1, col).setSouthConnected(true);
		}
	}

	private static class Board {
		private final Cell[][] data;

		Board(int boardSize) {
			data = new Cell[boardSize][boardSize];
		}

		void setCell(int row, int column, Cell cell) {
			data[row][column] = cell;
		}

		Cell getCell(int row, int col) {
			return data[row][col];
		}

		Cell[][] getCells() {
			return data;
		}
	}

	private enum Direction {
		NORTH,
		SOUTH;
	}

	private static class Cell {
		private String pipeIcon;
		private Boolean northConnected;
		private Boolean southConnected;

		Cell(String value) {
			this.pipeIcon = value;
		}

		Boolean getConnected(Direction direction) {
			return direction == Direction.NORTH ? northConnected : southConnected;
		}

		void setConnected(Direction direction, Boolean connected) {
			switch (direction) {
				case NORTH:
					this.northConnected = connected;
					break;
				case SOUTH:
					this.southConnected = connected;
					break;
			}
		}

		Boolean getSouthConnected() {
			return southConnected;
		}

		void setSouthConnected(Boolean southConnected) {
			this.southConnected = southConnected;
		}

		void resetConnections() {
			northConnected = null;
			southConnected = null;
		}

		boolean isNorth() {
			return pipeIcon.charAt(0) == '*' || EMPTY_GLASS.equals(pipeIcon) || FULL_GLASS.equals(pipeIcon);
		}

		boolean isSouth() {
			return pipeIcon.charAt(2) == '*' || EMPTY_GLASS.equals(pipeIcon) || FULL_GLASS.equals(pipeIcon);
		}

		boolean isWest() {
			return pipeIcon.charAt(3) == '*';
		}

		boolean isEast() {
			return pipeIcon.charAt(1) == '*';
		}

		boolean isNotDualConnected() {
			return northConnected == null || southConnected == null || !northConnected || !southConnected;
		}
	}

	private static class PipesGameTableModel extends DefaultTableModel {

		private final Board board;

		public PipesGameTableModel(int rowCount, int columnCount, Board board) {
			super(rowCount, columnCount);
			this.board = board;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return ImageIcon.class;
		}

		@Override
		public Object getValueAt(int row, int column) {
			final Cell cell = board.getCell(row, column);
			if (row == 0 || row == (BOARD_SIZE - 1) || cell.isNotDualConnected()) {
				return images.get(cell.pipeIcon);
			}
			return images.get(cell.pipeIcon + FILLED_PIPE_SUFFIX);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	}

	/**
	 * top scores controller. game independent preferable
	 */
	private static class TopScoresController {
		private static final String GAME_CENTER_URL = "http://www.elcoder.ru/gameCenter/scoreboard.php";
		private final String gameCode;

		private TopScoresController(String gameCode) {
			this.gameCode = gameCode;
		}

		java.util.List<ScoreItem> loadScores() throws IOException, XMLStreamException {
			final java.util.List<ScoreItem> scores = new ArrayList<>();
			final URL gameCenter = new URL(GAME_CENTER_URL + "?cmd=list&game=" + gameCode);
			final URLConnection yc = gameCenter.openConnection();
			parseListXML(yc.getInputStream(), scores);
			Collections.sort(scores);
			return scores;
		}

		/**
		 * add score to global score table
		 *
		 * @return true, if the score entered in the global score board
		 */
		boolean addScore(ScoreItem scoreItem) {
			// TODO: 04.06.2018 implement
			return false;
		}

		private void parseListXML(InputStream inputStream, java.util.List<ScoreItem> scores) throws XMLStreamException {
			ScoreItem scoreItem = null;
			final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
			final XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream);
			while (xmlEventReader.hasNext()) {
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()) {
					StartElement startElement = xmlEvent.asStartElement();
					if (startElement.getName().getLocalPart().equals("ScoreItem")) {
						scoreItem = new ScoreItem();
					} else if (startElement.getName().getLocalPart().equals("Name")) {
						xmlEvent = xmlEventReader.nextEvent();
						scoreItem.name = xmlEvent.asCharacters().getData().trim();
					} else if (startElement.getName().getLocalPart().equals("Score")) {
						xmlEvent = xmlEventReader.nextEvent();
						scoreItem.score = Integer.parseInt(xmlEvent.asCharacters().getData());
					} else if (startElement.getName().getLocalPart().equals("Time")) {
						xmlEvent = xmlEventReader.nextEvent();
						scoreItem.time = Integer.parseInt(xmlEvent.asCharacters().getData());
					}
				}
				if (xmlEvent.isEndElement()) {
					EndElement endElement = xmlEvent.asEndElement();
					if (endElement.getName().getLocalPart().equals("ScoreItem")) {
						scores.add(scoreItem);
					}
				}
			}
		}

		void showScores(JFrame parentFrame, List<ScoreItem> scores) {
			final JDialog frame = new JDialog(parentFrame, TOP_SCORES, true);
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			final String[] columnsHeader = new String[]{"name", "scores", "time"};
			final String[][] rowData = new String[scores.size()][3];
			int i = 0;
			for (ScoreItem score : scores) {
				rowData[i][0] = score.name;
				rowData[i][1] = String.valueOf(score.score);
				rowData[i][2] = formatTime(score.time);
				i++;
			}
			final JTable scoresTable = new JTable(new DefaultTableModel(rowData, columnsHeader) {
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			});
			final DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
			cellRenderer.setHorizontalAlignment(JLabel.CENTER);
			scoresTable.getColumnModel().getColumn(1).setCellRenderer(cellRenderer);
			scoresTable.getColumnModel().getColumn(2).setCellRenderer(cellRenderer);

			final Box contents = new Box(BoxLayout.Y_AXIS);
			JScrollPane comp = new JScrollPane(scoresTable);
			comp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
			contents.add(comp);
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
			buttonPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
			buttonPane.add(Box.createHorizontalGlue());
			final JButton okButton = new JButton("OK");
			buttonPane.add(okButton);
			okButton.addActionListener(e -> {
				frame.dispose();
			});
			contents.add(buttonPane);
			frame.setContentPane(contents);
			frame.setSize(500, 400);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		}

		public int saveScores(final ScoreItem scoreItem) throws IOException, XMLStreamException {
			final StringBuilder sb = new StringBuilder(GAME_CENTER_URL);
			sb.append("?cmd=add&game=").append(gameCode)
					.append("&name=").append(scoreItem.name)
					.append("&score=").append(scoreItem.score)
					.append("&time=").append(scoreItem.time)
					.append("&date=").append(formatDate());
			final URL gameCenter = new URL(sb.toString());
			final URLConnection yc = gameCenter.openConnection();
			return parseAddXML(yc.getInputStream());
		}

		private String formatDate() throws UnsupportedEncodingException {
			return URLEncoder.encode(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()), "UTF-8");
		}

		private int parseAddXML(InputStream inputStream) throws XMLStreamException {
			int boardPlace = -1;
			final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
			final XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream);
			while (xmlEventReader.hasNext()) {
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()) {
					StartElement startElement = xmlEvent.asStartElement();
					if (startElement.getName().getLocalPart().equals("Position")) {
						xmlEvent = xmlEventReader.nextEvent();
						boardPlace = Integer.parseInt(xmlEvent.asCharacters().getData());
					}
				}
			}
			return boardPlace;
		}

		private static class ScoreItem implements Comparable {
			private String name;
			private int score;
			private int time;

			public ScoreItem(int scores, String playerName, int time) {
				this.name = playerName;
				this.score = scores;
				this.time = time;
			}

			public ScoreItem() {

			}

			@Override
			public int compareTo(Object o) {
				if (o == null) {
					return -1;
				}
				final ScoreItem that = (ScoreItem) o;
				int compare = -Integer.compare(score, that.score);
				if (compare != 0) {
					return compare;
				}
				compare = Integer.compare(time, that.time);
				if (compare != 0) {
					return compare;
				}
				if (name != null)
					return name.compareToIgnoreCase(that.name);
				return -1;
			}
		}
	}
}

<?php
define("MAX_BOARD_SIZE", 20);
echo "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";

try {
    $cmd = get_parameter("cmd", '/[^a-z]/');
    $gameCode = get_parameter("game", '/[^A-Za-z0-9\-\.\!\?\s]/');

    if (!file_exists('./scores')) {
        mkdir('./scores', 0777, true);
    }

    $fileName = "./scores/$gameCode.txt";
    if (!file_exists($fileName)) {
        touch($fileName);
    }
    if ($cmd == "list") {
        show_leader_board($fileName, $gameCode);
    } else if ($cmd == "add") {
        $name = get_parameter("name", '/[^A-Za-z0-9]/');
        $score = get_parameter("score", '/[^0-9]/');
        $time = get_parameter("time", '/[^0-9]/');
        $date = get_parameter("date", '/[^0-9\.\:\s]/');
        add_score($fileName, $name, $time, $score, $date);
    } else {
        echo "<Error>Error 007</Error>";
        return;
    }

} catch (Exception $e) {
    echo "<Error>" . $e->getMessage() . "</Error>";
    return;
}

function get_parameter($name, $re)
{
    if (!isset($_GET[$name])) {
        throw new Exception("Parameter $name not found");
    }
    $result = $_GET[$name];
    if (preg_match($re, $result)) {
        throw new Exception("Format error, parameter $name");
    }
    return $result;
}

function add_score($fileName, $name, $time, $score, $date)
{
    $board = load_board($fileName);
    $line = array($name, $score, $time, $date);
    if (count($board) == MAX_BOARD_SIZE) {
        $best_result = compare_results($line, $board[count($board) - 1]);
        if ($best_result != -1) {
            echo "<Position>-1</Position>";
            return;
        }
    }
    $board[] = $line;
    usort($board, "compare_results");
    if (count($board) > MAX_BOARD_SIZE) {
        $board = array_slice($board, 0, MAX_BOARD_SIZE);
    }
    save_board($fileName, $board);
    $best_result = array_search($line, $board) + 1;
    echo "<Position>$best_result</Position>";
}

function save_board($fileName, $board)
{
    $result = "";
    foreach ($board as $line) {
        $result .= implode("|", $line) . "\n";
    }
    file_put_contents($fileName, $result);
}

/**
 * @param $fileName
 * @param $gameCode
 */
function show_leader_board($fileName, $gameCode)
{
    $lines = load_board($fileName);
    $res = "<ScoreBoard>\n    <Game>$gameCode</Game>\n";
    foreach ($lines as $line) {
        $name = $line[0];
        $score = $line[1];
        $time = $line[2];
        $date = $line[3];
        $res .= "    <ScoreItem>
        <Name>$name</Name>
        <Score>$score</Score>
        <Time>$time</Time>
        <Date>$date</Date>
	</ScoreItem>\n";
    }
    echo $res . "</ScoreBoard>";
}

function compare_results($a, $b)
{
    $result = ($a[1] > $b[1]) ? -1 : (($a[1] < $b[1]) ? 1 : 0);
    if ($result != 0) {
        return $result;
    }
    $result = ($a[2] > $b[2]) ? 1 : (($a[2] < $b[2]) ? -1 : 0);
    if ($result == 0) {
        $result = strcmp($a[0], $b[0]);
    }
    return $result;
}

function load_board($fileName)
{
    $content = file_get_contents($fileName);
    $lines = explode("\n", $content);
    $result = array();
    foreach ($lines as $line) {
        $parts = explode("|", $line);
        if (count($parts) == 4) {
            $result[] = $parts;
        }
    }
    usort($result, "compare_results");
    if (count($result) > MAX_BOARD_SIZE) {
        $result = array_slice($result, 0, MAX_BOARD_SIZE);
    }
    return $result;
}

?>
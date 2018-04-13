<?
error_reporting(E_ALL);
//set_time_limit(40);

$logFile = "log.txt";
$fh = fopen($logFile, 'w');

echo "Server Begin...";
fwrite($fh, "Server Begin...");

$address = 0;
$port = 1234;

$socket = stream_socket_server("udp://" . $address . ":" . $port, $errno, $errstr, STREAM_SERVER_BIND);
if (!$socket) {
    die("$errstr ($errno)");
}
	
do {
	$packet = stream_socket_recvfrom($socket, 128, 0, $peer);
	echo "$peer -- " . $packet . "\n";
  fwrite($fh, "$peer -- " . $packet . "\n");
	stream_socket_sendto($socket, $whoami . '/' . date('Y-m-d H:i:s') . '/' . $peer . "\r\n", 0, $peer);
	echo '> REPLY SENT!' . "\n";
  fwrite($fh, '> REPLY SENT!' . "\n");
} while ($packet !== false);

fwrite ($fh, "\nEnd at: ".time());
fclose ($fh);

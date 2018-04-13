<?php
error_reporting(E_ALL);
//set_time_limit(40);

$logFile = "log.txt";
$fh = fopen($logFile, 'w');

mylog("Server Begin...");

$address = 0;
$port = 1234;
$socket = stream_socket_server("udp://" . $address . ":" . $port, $errno, $errstr, STREAM_SERVER_BIND);

if (!$socket) {
    die("$errstr ($errno)");
    fwrite ($fh, "\nEnd at: ".time());
    fclose ($fh);
}
	
do {
    $packet = stream_socket_recvfrom($socket, 128, 0, $peer);
    mylog("Receive Packet:" . $packet);
    stream_socket_sendto($socket, $whoami . '/' . date('Y-m-d H:i:s') . '/' . $peer . "\r\n", 0, $peer);
} while ($packet !== false);

function mylog($data) {
    echo $data;
    fwrite($fh, $data);
    return 0;
}

fwrite ($fh, "\nEnd at: ".time());
fclose ($fh);

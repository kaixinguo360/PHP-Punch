<?php
    
$logFile = "log.txt";
$fh = fopen($logFile, 'w');
$address = 0;
$port = 1234;
$socket = stream_socket_server("udp://" . $address . ":" . $port, $errno, $errstr, STREAM_SERVER_BIND);

init();

if (!$socket) {
    die("$errstr ($errno)");
    fwrite ($fh, "\nEnd at: ".time());
    fclose ($fh);
}
	
do {
    $packet = stream_socket_recvfrom($socket, 128, 0, $peer);
    receive_packet($packet, $peer);
} while (true);


/** Functions **/

function receive_packet($packet, $peer) {
    global $socket;
    mylog("Receive Packet:" . $packet);
    stream_socket_sendto($socket, $whoami . '/' . date('Y-m-d H:i:s') . '/' . $peer . "\r\n", 0, $peer);
}

function init() {
    error_reporting(E_ALL);
    //set_time_limit(40);
    
    require_once 'Socket.class.php';
    
    mylog("Server Begin...");
}

function mylog($data) {
    global $fh;
    echo $data . "<br>";
    fwrite($fh, $data . "\n");
}

fwrite ($fh, "\nEnd at: ".time());
fclose ($fh);

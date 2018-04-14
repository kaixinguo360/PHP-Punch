<?php

$myloger;
$socket;
$proxys;
init();

do {
    $packet = stream_socket_recvfrom($socket, 128, 0, $peer);
    receive_packet($packet, $peer);
} while (true);


/** Functions **/

function receive_packet($packet, $peer) {
    global $socket, $proxys, $myloger;
    if(!$proxys[$peer]) {
        $proxys[$peer] = new Proxy($socket, $peer, $myloger);
    } else {
        $proxys[$peer] -> receive($packet);
    }
}

function init() {
	global $myloger, $socket;
	
    error_reporting(E_ALL);
    
    require_once 'Proxy.class.php';
    require_once 'MyLoger.class.php';
    
    $myloger = new MyLoger("log.txt");
    
    $address = 0;
    $port = 1234;
    $socket = stream_socket_server("udp://" . $address . ":" . $port, $errno, $errstr, STREAM_SERVER_BIND);
    
    if (!$socket) {
        die("$errstr ($errno)");
    }
    
    mylog("Server Begin...");
}

function mylog($data) {
    global $myloger;
    $myloger -> log($data);
}

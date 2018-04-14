<?php


/** Init **/

error_reporting(E_ALL);
require_once 'Proxy.class.php';
require_once 'MyLoger.class.php';

$myloger = new MyLoger("log.txt");
$socket = getSocket(0, 1234);
$proxys;


/** Main Loop **/

do {
    $packet = stream_socket_recvfrom($socket, 128, 0, $peer);
    if(!$proxys[$peer]) {
        $proxys[$peer] = new Proxy($socket, $peer, $myloger);
    } else {
        $proxys[$peer] -> receive($packet);
    }
} while (true);


/** Functions **/

function getSocket($address, $port) {
	
    $socket = stream_socket_server("udp://" . $address . ":" . $port, $errno, $errstr, STREAM_SERVER_BIND);
    
    if (!$socket) {
        die("$errstr ($errno)");
    }
    
    mylog("Server Begin...");
    
    return $socket;
}

function mylog($data) {
    global $myloger;
    $myloger -> log($data);
}

<?php


/** Init **/

error_reporting(E_ALL);
set_time_limit(0);
define("LOG_LEVEL", 2);
define("TTL_PROXY", 15);
define("TTL_RELAY", 15);
require_once 'Proxy.class.php';
require_once 'MyLoger.class.php';
require_once 'Relay.class.php';

$myloger = new MyLoger("log.txt");
$socket = getSocket(0, 2018);
$proxys;
$relays;


/** Test **/

new Relay(0,0, null);


/** Main Loop **/

do {
    $beginTime = microtime(true);
    $packet = stream_socket_recvfrom($socket, 128, 0, $peer);
    if(!checkRelay($peer, $packet)) {
        if($packet == "WHOAMI") {
            stream_socket_sendto($socket, $peer, 0, $peer);
            mylog("WHO AM I : " . $peer);
        } else {
            updateUser();
            if(!$proxys[$peer])
                $proxys[$peer] = new Proxy($socket, $peer);
            $proxys[$peer] -> receive($packet);
        }
    }
    if(microtime(true) - $beginTime < 100000) {
        usleep(100000);
    }
} while (true);


/** Functions **/

function checkRelay($peer, $packet) {
    global $relays;
    foreach ($relays as $address => $relay) {
        if($address == $peer) {
            $relay -> receive($peer, $packet);
            return true;
        } else {
            if(!$relay -> isAlive()) {
                mylog("Relay Close ! " . $relay -> peer1 . " <==> " . $relay -> peer2);
                removeRelay($relay);
            }
        }
    }
    return false;
}

function createRelay($peer1, $peer2) {
    global $relays, $socket;
    if($relays[$peer1]) {
        return $relays[$peer1];
    } else if($relays[$peer2]){
        return $relays[$peer2];
    } else {
        $relay = new Relay($peer1, $peer2, $socket);
        $relays[$peer1] = $relay;
        $relays[$peer2] = $relay;
        return $relay;
    }
}

function removeRelay($relay) {
    global $relays;
    unset($relays[$relay -> peer1]);
    unset($relays[$relay -> peer2]);
}

function getProxy($name) {
    global $proxys;
    foreach ($proxys as $address => $proxy) {
        if($proxy -> name == $name) {
            return $proxy;
        }
    }
    return NULL;
}

function getList() {
    global $proxys;
    $list;
    foreach ($proxys as $address => $proxy) {
        if($proxy -> status >= 2) {
            $list = $list . $proxy -> name . "\n";
        }
    }
    return $list;
}

function updateUser() {
    global $proxys;
    foreach ($proxys as $address => $proxy) {
        if(!$proxy -> isAlive()) {
            mylog($proxys[$address] -> name . " -X- Lost !");
            unset($proxys[$address]);
        }
    }
}

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

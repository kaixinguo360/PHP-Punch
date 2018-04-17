<?php
class Relay{
    
    /* 成员变量 */
    var $peer1;
    var $peer2;
    var $socket;
    var $lastHB;
    
    /* 构造函数 */
    function __construct($peer1, $peer2, $socket) {
        $this -> lastHB = time();
        
        $this -> peer1 = $peer1;
        $this -> peer2 = $peer2;
        
        $this -> socket = $socket;
        
        $this -> mylog("Create New Relay " . $peer1 . " <==> " . $peer2);
    }
    
    /* 成员函数 */
    function receive($peer, $data){
        $this -> lastHB = time();
        mylog($peer . "===>" . $data);
        if($peer == $this -> peer1) {
            $this -> send($this -> peer2, $data);
        } else if($peer == $this -> peer2) {
            $this -> send($this -> peer1, $data);
        }
    }
    
    function send($peer, $data){
        $this -> mylog($peer . " <=== " . $data, 1);
        stream_socket_sendto($this -> socket, $data, 0, $peer);
    }
    
    function isAlive(){
        $time = time() - $this -> lastHB;
        
        if($time < TTL_RELAY) {
            return true;
        } else {
            return false;
        }
    }
    
    function mylog($data, $p = 0){
        if($p < LOG_LEVEL) {
            mylog($data);
        }
    }
}

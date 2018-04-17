<?php
class MyLoger {
    
    /* 成员变量 */
    var $fh = null;
    
    /* 构造函数 */
    function __construct($logFile = null) {
        if($logFile != null) {
            $this -> fh = fopen($logFile, 'w');
        }
    }
    
    /* 析构函数 */
    function __destruct(){
        fwrite ($this -> fh, "\nEnd at: ".time());
        fclose ($this -> fh);
    }
    
    /* 成员函数 */
    function log($data) {
        //$date = date('Y-m-d H:i:s');
        $date = date('H:i:s');
        $data = $date . " " . $data;
        echo $data . "\n";
        if($this -> fh != null) {
            fwrite($this -> fh, $data . "\n");
        }
    }
}

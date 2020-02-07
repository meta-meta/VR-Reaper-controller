using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class Theremin : MonoBehaviour
{
    private OscOut _oscOutMax;

    public GameObject ampObj;
    public GameObject freqObj;
    public GameObject controllerLeft;
    public GameObject controllerRight;

    private string _addr = "/theremin";
    private OscMessage _freq;
    private OscMessage _amp;
    private OscMessage _priX;
    private OscMessage _secX;
    private OscMessage _priY;
    private OscMessage _secY;
    
    
	// Use this for initialization
	void Start ()
	{
		_oscOutMax = GameObject.Find("OSC").GetComponents<OscOut>().First(oscOUt => oscOUt.port == 8000);

//        ampObj = GameObject.Find("amp");
//        freqObj = GameObject.Find("freq");
//        controllerLeft = GameObject.Find("LeftPointer");
//        controllerRight = GameObject.Find("RightPointer");
        
        _freq = new OscMessage($"{_addr}/freq");
        _amp = new OscMessage($"{_addr}/amp");
        _priX = new OscMessage($"{_addr}/pri/x");
        _priY = new OscMessage($"{_addr}/pri/y");
        _secX = new OscMessage($"{_addr}/sec/x");
        _secY = new OscMessage($"{_addr}/sec/y");
	}

	float dist(GameObject go1, GameObject go2) =>
		Vector3.Distance(go1.transform.position, go2.transform.position);

    // Update is called once per frame
    void Update ()
    {
        _freq.Set(0, dist(freqObj, controllerRight));
        _amp.Set(0, dist(ampObj, controllerLeft)); 
        
        var priThumb = OVRInput.Get(OVRInput.Axis2D.PrimaryThumbstick);
        var secThumb = OVRInput.Get(OVRInput.Axis2D.SecondaryThumbstick);
        _priX.Set(0, priThumb.x);
        _priY.Set(0, priThumb.y);
        _secX.Set(0, secThumb.x);
        _secY.Set(0, secThumb.y);

        _oscOutMax.Send(_freq);
        _oscOutMax.Send(_amp);
        _oscOutMax.Send(_priX);
        _oscOutMax.Send(_priY);
        _oscOutMax.Send(_secX);
        _oscOutMax.Send(_secY);
    }
}
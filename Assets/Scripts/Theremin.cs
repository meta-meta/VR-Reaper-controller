using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class Theremin : MonoBehaviour
{
    private OscOut _oscOutMax;

    public GameObject ampObj;
    public GameObject freqObj;
    public GameObject pointerLeft;
    public GameObject pointerRight;
    private OVRInput.Controller controllerLeft;
    private OVRInput.Controller controllerRight;

    private string _addr = "/theremin";
    private OscMessage freq;
    private OscMessage amp;
    private OscMessage priX;
    private OscMessage priY;
    private OscMessage secX;
    private OscMessage secY;
    private OscMessage click;
    private OscMessage noise;
    private OscMessage voice;

    private bool isOn;
    
	// Use this for initialization
	void Start ()
	{
		_oscOutMax = GameObject.Find("OSC").GetComponent<OscManager>().OscOutMaxMsp;

        freq = new OscMessage($"{_addr}/freq");
        amp = new OscMessage($"{_addr}/amp");
        priX = new OscMessage($"{_addr}/pri/x");
        priY = new OscMessage($"{_addr}/pri/y");
        secX = new OscMessage($"{_addr}/sec/x");
        secY = new OscMessage($"{_addr}/sec/y");
        click = new OscMessage($"{_addr}/click");
        click.Set(0, 1);
        noise = new OscMessage($"{_addr}/noise");
        voice = new OscMessage($"{_addr}/voice");

        controllerLeft = pointerLeft.GetComponent<Pointer>().Controller;
        controllerRight = pointerRight.GetComponent<Pointer>().Controller;
	}

	float dist(GameObject go1, GameObject go2) =>
		Vector3.Distance(go1.transform.position, go2.transform.position);

    // Update is called once per frame
    void Update()
    {
	    if (isOn)
	    {
		    freq.Set(0, dist(freqObj, pointerRight));
		    amp.Set(0, dist(ampObj, pointerLeft));

		    var priThumb = OVRInput.Get(OVRInput.Axis2D.PrimaryThumbstick, controllerLeft);
		    var secThumb = OVRInput.Get(OVRInput.Axis2D.PrimaryThumbstick, controllerRight);
		    priX.Set(0, priThumb.x);
		    priY.Set(0, priThumb.y);
		    secX.Set(0, secThumb.x);
		    secY.Set(0, secThumb.y);
		    noise.Set(0, OVRInput.Get(OVRInput.Axis1D.PrimaryIndexTrigger, controllerLeft));
		    voice.Set(0, OVRInput.Get(OVRInput.Axis1D.PrimaryIndexTrigger, controllerRight));

		    _oscOutMax.Send(freq);
		    _oscOutMax.Send(amp);
		    _oscOutMax.Send(priX);
		    _oscOutMax.Send(priY);
		    _oscOutMax.Send(secX);
		    _oscOutMax.Send(secY);
		    _oscOutMax.Send(noise);
		    _oscOutMax.Send(voice);

		    if (OVRInput.GetDown(OVRInput.Button.One, controllerLeft)) _oscOutMax.Send(click);
	    }

	    if (GetComponent<Manipulate>().IsTouching && (OVRInput.GetDown(OVRInput.Button.PrimaryIndexTrigger) ||
	                                                  OVRInput.GetDown(OVRInput.Button.SecondaryIndexTrigger)))
	    {
		    isOn = !isOn;
		    ampObj.GetComponent<MeshRenderer>().material.SetFloat("_RimPower", isOn ? 0.5f : 2);
		    freqObj.GetComponent<MeshRenderer>().material.SetFloat("_RimPower", isOn ? 0.5f : 2);
	    }
    }
}
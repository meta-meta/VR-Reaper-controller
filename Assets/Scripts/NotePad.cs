using System.Collections;
using System.Collections.Generic;
using System.Linq;
using OscSimpl;
using UnityEngine;

public class NotePad : MonoBehaviour
{
    private OscOut _oscOutMax;
    public string oscAddr;
    public int note;

    private GameObject _touchController;
    private OVRInput.Controller _controllerMask;

    // Start is called before the first frame update
    void Start()
    {
        _oscOutMax = GameObject.Find("OSC").GetComponent<OscManager>().OscOutMaxMsp;

        var n = note % 12;
        gameObject.GetComponentInChildren<TextMesh>().text = n == 11 ? "Ɛ" : n == 10 ? "૪" : n.ToString();
    }

    // Update is called once per frame
    void Update()
    {
        /* TODO:
             * show interval between hands when close to notes
         *   * colored eggs are "shells" over the actual notes. They can all be shifted (transpose) to make the new red egg wrap around static D rather than static C
         *   * ability to select a note and some scale or chord mask on top so that the out of key eggs cannot be hit
         */
        
        // TODO: individual sustain for each hand (need 2 separate instances of pianoteq)
        if ((OVRInput.GetDown(OVRInput.Button.PrimaryIndexTrigger) ||
            OVRInput.GetDown(OVRInput.Button.SecondaryIndexTrigger)))
        {
            _oscOutMax.Send(new OscMessage($"{oscAddr}/cc").Add(64).Add(127));
        }
        
        if ((OVRInput.GetUp(OVRInput.Button.PrimaryIndexTrigger) ||
             OVRInput.GetUp(OVRInput.Button.SecondaryIndexTrigger)))
        {
            _oscOutMax.Send(new OscMessage($"{oscAddr}/cc").Add(64).Add(0));
        }

    }

    private void OnTriggerEnter(Collider other)
    {
        var pointer = other.GetComponent<Pointer>();

        if (pointer != null)
        {
            _touchController = other.gameObject;
            _controllerMask = pointer.Controller;

//            var trigger = OVRInput.Get(OVRInput.Axis1D.PrimaryHandTrigger, pointer.Controller);
            var controllerVel = OVRInput.GetLocalControllerVelocity(pointer.Controller).magnitude;
            var vel = Mathf.Lerp(0, 1, controllerVel / 3.5f);
//            var comboVel = vel + trigger / 3f;

            Vibe(vel, pointer.Controller);
            _oscOutMax.Send(
                new OscMessage($"{oscAddr}/note")
                    .Add(note)
                    .Add(Mathf.FloorToInt(vel * 127f))
            );
        }
    }
    
    private void OnTriggerExit(Collider other)
    {
        var pointer = other.GetComponent<Pointer>();
        if (pointer != null && _controllerMask == pointer.Controller)
        {
            Vibe(0.1f, pointer.Controller);
            _oscOutMax.Send(
                new OscMessage($"{oscAddr}/note")
                    .Add(note)
                    .Add(0)
            );
            _touchController = null;
            _controllerMask = OVRInput.Controller.None;
        }
    }

    void Vibe(float strength, OVRInput.Controller controller)
    {
        OVRInput.SetControllerVibration(0.1f, strength, controller);
        StartCoroutine(VibeOff(0.01f, controller));
    }

    IEnumerator VibeOff(float timeout, OVRInput.Controller controller)
    {
        while (true)
        {
            yield return null;
            timeout -= Time.deltaTime;
            if (timeout <= 0f)
            {
                OVRInput.SetControllerVibration(0, 0, controller);
                break;
            }
        }
    }
}
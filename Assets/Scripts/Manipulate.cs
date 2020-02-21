using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class Manipulate : MonoBehaviour
{
    public bool IsPinching { get; private set; }
    public bool IsGrabbing { get; private set; }
    public bool IsTouching { get; private set; }

    private GameObject _touchControllerGrab;
    private OVRInput.Controller _controllerMaskGrab = OVRInput.Controller.None;
    private Transform _parentAtGrab;
    
    private float _controllerDistanceAtPinchStart;
    private GameObject _touchControllerPinch;
    private OVRInput.Controller _controllerMaskPinch = OVRInput.Controller.None;
    private Vector3 _scaleAtPinchStart;

    private Save _save;

    void Start()
    {
        _save = GameObject.Find("App").GetComponent<Save>();
    }

    // Update is called once per frame
    void Update()
    {
        if (
            IsTouching &&
            _controllerMaskGrab != OVRInput.Controller.None &&
            OVRInput.GetDown(OVRInput.Button.PrimaryHandTrigger, _controllerMaskGrab)
        )
        {// Grab start
            IsGrabbing = true;
            _parentAtGrab = transform.parent;
            transform.SetParent(_touchControllerGrab.transform, true);
        }

        if (IsTouching && OVRInput.GetDown(OVRInput.Button.One, _controllerMaskGrab))
        {
            _save.SavePosition(gameObject);
        }
        
        if (
            IsGrabbing &&
            _controllerMaskPinch != OVRInput.Controller.None &&
            OVRInput.GetDown(OVRInput.Button.PrimaryHandTrigger, _controllerMaskPinch)
        )
        {// Pinch start
            IsPinching = true;
            Vibe(0.5f, _controllerMaskGrab);
            Vibe(0.2f, _controllerMaskPinch);
            _scaleAtPinchStart = transform.localScale;
            _controllerDistanceAtPinchStart = (_touchControllerGrab.transform.position - _touchControllerPinch.transform.position).magnitude;
        }

        if (IsPinching)
        {// is Pinching
            var mag = (_touchControllerGrab.transform.position - _touchControllerPinch.transform.position)
                .magnitude / _controllerDistanceAtPinchStart;
            transform.localScale = _scaleAtPinchStart * mag;
        }

        if (IsPinching && (
                OVRInput.GetUp(OVRInput.Button.PrimaryHandTrigger, _controllerMaskPinch)
                || OVRInput.GetUp(OVRInput.Button.PrimaryHandTrigger, _controllerMaskGrab)
            ))
        {// Pinch end
            _controllerMaskPinch = OVRInput.Controller.None;
            IsPinching = false;
        }
        
        if (IsGrabbing)
        {// is Grabbing
            
        }

        if (IsGrabbing && OVRInput.GetUp(OVRInput.Button.PrimaryHandTrigger, _controllerMaskGrab))
        {// Grab end
            transform.SetParent(_parentAtGrab, true);
            IsGrabbing = false;
        }

        
    }

    private void OnTriggerEnter(Collider other)
    {
        var pointer = other.GetComponent<Pointer>();
        if (!IsTouching && !IsGrabbing && pointer != null)
        { // Touch start; Grab is possible
            IsTouching = true;
            _touchControllerGrab = other.gameObject;
            _controllerMaskGrab = pointer.Controller;
            Vibe(0.2f, _controllerMaskGrab);
        } 
        
        if (IsGrabbing && pointer != null)
        { // Pinch is possible
            _controllerMaskPinch = pointer.Controller;
            _touchControllerPinch = other.gameObject;
            Vibe(0.2f, _controllerMaskPinch);
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

    private void OnTriggerExit(Collider other)
    {
        if (IsTouching && other.gameObject == _touchControllerGrab)
        {
            IsTouching = false;

            if (!IsGrabbing) // Only necessary because OnTriggerExit can be called between frames
            {
                _touchControllerGrab = null;
                _controllerMaskGrab = OVRInput.Controller.None;

                var pointer = other.GetComponent<Pointer>();
                Vibe(0.1f, pointer.Controller);
            }
        }
    }
}
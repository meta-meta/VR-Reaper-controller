using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class Manipulate : MonoBehaviour
{
    public bool IsGrabbed { get; private set; }
    public bool IsTouched { get; private set; }

    private GameObject _touchController;
    private OVRInput.Controller _controllerMask = OVRInput.Controller.None;
    private Vector3 _offsetAtGrab;
    
    // Update is called once per frame
    void Update()
    {
        if (
            IsTouched &&
            _controllerMask != OVRInput.Controller.None &&
            OVRInput.GetDown(OVRInput.Button.PrimaryHandTrigger, _controllerMask)
        )
        {
            // Grab start
            IsGrabbed = true;
            _offsetAtGrab = transform.position - _touchController.transform.position;
        }

        if (IsGrabbed && OVRInput.GetUp(OVRInput.Button.PrimaryHandTrigger, _controllerMask))
        {
            // Grab end
            IsGrabbed = false;
        }

        if (IsGrabbed)
        {
            // is Grabbing
            transform.position = _touchController.transform.position + _offsetAtGrab;
        }
    }

    private void OnTriggerEnter(Collider other)
    {
        var pointer = other.GetComponent<Pointer>();

        if (!IsTouched && pointer != null)
        {
            IsTouched = true;
            _touchController = other.gameObject;
            _controllerMask = pointer.Controller;

            if (!IsGrabbed)
            {
                Vibe(0.2f, pointer.Controller);
            }
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
        if (IsTouched && other.gameObject == _touchController)
        {
            IsTouched = false;

            if (!IsGrabbed) // Only necessary because OnTriggerExit can be called between frames
            {
                _touchController = null;
                _controllerMask = OVRInput.Controller.None;

                var pointer = other.GetComponent<Pointer>();
                Vibe(0.1f, pointer.Controller);
            }
        }
    }
}
using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class HeadDock : MonoBehaviour
{
    private Manipulate _manipulate;
    private bool _isHeadGrabbed
    {
        get
        {
            if (!_manipulate) _manipulate = transform.GetChild(0).GetComponent<Manipulate>();
            return _manipulate.IsGrabbing;
        }
    }

    private ListenerIEM _cameraEars;
    private ListenerIEM _headEars;

    private Collider _headCollider;
    
    // Start is called before the first frame update
    void Start()
    {
        _cameraEars = GameObject.Find("CenterEyeAnchor").GetComponent<ListenerIEM>();
        _headEars = transform.GetChild(0).GetComponent<ListenerIEM>();
        _headCollider = transform.GetChild(0).GetComponent<Collider>();
    }

    private bool _wasHeadGrabbed;
    void Update()
    {
        if (_isHeadGrabbed)
        {
            _wasHeadGrabbed = true;
        }
        else
        {
            if (_wasHeadGrabbed)
            {
                _wasHeadGrabbed = false;
                if (_isDocked)
                {
                    transform.GetChild(0).transform.SetPositionAndRotation(transform.position, transform.rotation);
                }
            }
        }
    }

    private void OnTriggerEnter(Collider other)
    {
        if (other == _headCollider && _isHeadGrabbed)
        {
            _isDocked = true;
            Vibe(0.5f, OVRInput.Controller.All);
            _cameraEars.enabled = true;
            _headEars.enabled = false;
        }
    }

    private bool _isDocked;
    private void OnTriggerExit(Collider other)
    {
        if (other == _headCollider && _isHeadGrabbed)
        {
            _isDocked = false;
            _cameraEars.enabled = false;
            _headEars.enabled = true;
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

using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class BinauralHead : MonoBehaviour
{
    private int _prevTrackNumSceneRotator;
    public int TrackNumSceneRotator;
    
    private int _prevTrackNumRoomEncoder;
    public int TrackNumRoomEncoder;
    
    private OscIn _oscInReaper;
    private OscOut _oscOutReaper;
    
    private Vector3 _pos;

    private OscMessage _listenerX;
    private OscMessage _listenerY;
    private OscMessage _listenerZ;
    
    private Manipulate _manipulate;
    private bool _isGrabbed
    {
        get
        {
            if (!_manipulate) _manipulate = gameObject.GetComponent<Manipulate>();
            return _manipulate.IsGrabbing;
        }
    }
    
    void Start()
    {
        _oscOutReaper = GameObject.Find("OSC").GetComponents<OscOut>().First(oscOut => oscOut.port == 8001);
        _oscInReaper = GameObject.Find("OSC").GetComponents<OscIn>().First(oscIn => oscIn.port == 7001);
        RemapOsc();
    }

    void RemapOsc()
    {
        if (TrackNumRoomEncoder > 0)
        {
            if (_prevTrackNumRoomEncoder > 0)
            {
                _oscInReaper.UnmapAll($"/track/{_prevTrackNumRoomEncoder}/fx/1/fxparam/11/value");
                _oscInReaper.UnmapAll($"/track/{_prevTrackNumRoomEncoder}/fx/1/fxparam/12/value");
                _oscInReaper.UnmapAll($"/track/{_prevTrackNumRoomEncoder}/fx/1/fxparam/13/value");
            }
            
            _oscInReaper.MapFloat($"/track/{TrackNumRoomEncoder}/fx/1/fxparam/11/value", OnReceiveListenerX );
            _oscInReaper.MapFloat($"/track/{TrackNumRoomEncoder}/fx/1/fxparam/12/value", OnReceiveListenerY );
            _oscInReaper.MapFloat($"/track/{TrackNumRoomEncoder}/fx/1/fxparam/13/value", OnReceiveListenerZ );
                    
            _listenerX = new OscMessage($"/track/{TrackNumRoomEncoder}/fx/1/fxparam/11/value");
            _listenerY = new OscMessage($"/track/{TrackNumRoomEncoder}/fx/1/fxparam/12/value");
            _listenerZ = new OscMessage($"/track/{TrackNumRoomEncoder}/fx/1/fxparam/13/value");
        }
    }
    
    static float Scale(float inMin, float inMax, float outMin, float outMax, float val) =>
        Mathf.Lerp(
            outMin,
            outMax,
            Mathf.InverseLerp(inMin, inMax, val));
    
    void OnReceiveListenerX(float val)
    {
        if (!_isGrabbed)
        {
            transform.position = new Vector3(
                Scale(0, 1f, -15f, 15f, val),
                transform.position.y,
                transform.position.z);
        }
    }
    
    void OnReceiveListenerY(float val) // IEM Y = Unity Z
    {
        if (!_isGrabbed)
        {
            transform.position = new Vector3(
                transform.position.x,
                transform.position.y,
                Scale(0, 1f, -15f, 15f, val));
        }
    }
    
    void OnReceiveListenerZ(float val) // IEM Z = Unity Y
    {
        if (!_isGrabbed)
        {
            
            transform.position = new Vector3(
                transform.position.x,
                Scale(0, 1f, -10f, 10f, val),
                transform.position.z);
        }
    }
    
    private void SendPositionToReaper()
    {
        _pos = transform.position;
        _listenerX.Set(0, Scale(-15f, 15f, 0f, 1f, _pos.x));
        _listenerY.Set(0, Scale(-15f, 15f, 0f, 1f, _pos.z)); // IEM Y = Unity Z
        _listenerZ.Set(0, Scale(-10f, 10f, 0f, 1f, _pos.y)); // IEM Z = Unity Y
        _oscOutReaper.Send(_listenerX);
        _oscOutReaper.Send(_listenerY);
        _oscOutReaper.Send(_listenerZ);
    }
    
    void Update()
    {
        if (_isGrabbed)
        {
            SendPositionToReaper();
        }
    }
}

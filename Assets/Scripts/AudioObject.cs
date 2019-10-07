using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

[ExecuteInEditMode]
public class AudioObject : MonoBehaviour
{
    [Range(0, 11)]
    public int Color;

    public bool FindTrack;
    private int _prevTrackNum;
    public int TrackNum;
    
    private Manipulate _manipulate;
    private bool _isGrabbed
    {
        get
        {
            if (!_manipulate) _manipulate = gameObject.GetComponent<Manipulate>();
            return _manipulate.IsGrabbed;
        }
    }

    private GameObject _nametag;

    private OscIn _oscInReaper;
    private OscOut _oscOutReaper;
    
    private OscMessage _listenerX;
    private OscMessage _listenerY;
    private OscMessage _listenerZ;
    
    private OscMessage _roomX;
    private OscMessage _roomY;
    private OscMessage _roomZ;
    
    private Vector3 _pos;

    private OscMessage _sourceX;
    private OscMessage _sourceY;
    private OscMessage _sourceZ;
    
    private void Start()
    {
        _oscOutReaper = GameObject.Find("OSC").GetComponents<OscOut>().First(oscOut => oscOut.port == 8001);
        _oscInReaper = GameObject.Find("OSC").GetComponents<OscIn>().First(oscIn => oscIn.port == 7001);
//        RemapOsc();
    }

    void RemapOsc()
    {
        if (TrackNum > 0)
        {
            if (_prevTrackNum > 0)
            {
                _oscInReaper.UnmapAll($"/track/{_prevTrackNum}/fx/1/fxparam/8/value");
                _oscInReaper.UnmapAll($"/track/{_prevTrackNum}/fx/1/fxparam/9/value");
                _oscInReaper.UnmapAll($"/track/{_prevTrackNum}/fx/1/fxparam/10/value");
            }
            
            _oscInReaper.MapFloat($"/track/{TrackNum}/fx/1/fxparam/8/value", OnReceiveSourceX );
            _oscInReaper.MapFloat($"/track/{TrackNum}/fx/1/fxparam/9/value", OnReceiveSourceY );
            _oscInReaper.MapFloat($"/track/{TrackNum}/fx/1/fxparam/10/value", OnReceiveSourceZ );
                    
            _sourceX = new OscMessage($"/track/{TrackNum}/fx/1/fxparam/8/value");
            _sourceY = new OscMessage($"/track/{TrackNum}/fx/1/fxparam/9/value");
            _sourceZ = new OscMessage($"/track/{TrackNum}/fx/1/fxparam/10/value");
        }
    }

    void OnReceiveSourceX(float val)
    {
        if (!_isGrabbed)
        {
            transform.position = new Vector3(
                Scale(0, 1f, -15f, 15f, val),
                transform.position.y,
                transform.position.z);
        }
    }
    
    void OnReceiveSourceY(float val) // IEM Y = Unity Z
    {
        if (!_isGrabbed)
        {
            transform.position = new Vector3(
                transform.position.x,
                transform.position.y,
                Scale(0, 1f, -15f, 15f, val));
        }
    }
    
    void OnReceiveSourceZ(float val) // IEM Z = Unity Y
    {
        if (!_isGrabbed)
        {
            
            transform.position = new Vector3(
                transform.position.x,
                Scale(0, 1f, -10f, 10f, val),
                transform.position.z);
        }
    }

    private string _name;
    private void UpdateNametag()
    {
        if (_name != gameObject.name)
        {
            if (!_nametag) _nametag = transform.Find("Nametag").gameObject;
            _name = gameObject.name;
            _nametag.GetComponent<TextMesh>().text = _name;
        }
    }

    private void OnValidate()
    {
        var meshRenderer = GetComponent<MeshRenderer>();
        if (meshRenderer.material.name != Color.ToString())
        {
            Debug.Log(Color);
            meshRenderer.material = Resources.Load<Material>($"Materials/AudioObject/{Color}");
        }

        if (FindTrack)
        {
            FindTrack = false;
            Debug.Log("TODO: Find track from Reaper");
            
            // TODO: get total # tracks from Reaper
            // forEach track, get the name. If track name matches GameObject name, set track number
            
            if (TrackNum > 0 && TrackNum != _prevTrackNum)
            {
                RemapOsc();
                _prevTrackNum = TrackNum;
            }
        }
    }

    static float Scale(float inMin, float inMax, float outMin, float outMax, float val) =>
        Mathf.Lerp(
            outMin,
            outMax,
            Mathf.InverseLerp(inMin, inMax, val));

    private void SendPositionToReaper()
    {
        _pos = transform.position;
        _sourceX.Set(0, Scale(-15f, 15f, 0f, 1f, _pos.x));
        _sourceY.Set(0, Scale(-15f, 15f, 0f, 1f, _pos.z)); // IEM Y = Unity Z
        _sourceZ.Set(0, Scale(-10f, 10f, 0f, 1f, _pos.y)); // IEM Z = Unity Y
        _oscOutReaper.Send(_sourceX);
        _oscOutReaper.Send(_sourceY);
        _oscOutReaper.Send(_sourceZ);
    }
    
    void Update()
    {
        UpdateNametag();

        if (_isGrabbed)
        {
            SendPositionToReaper();
        }
    }
}
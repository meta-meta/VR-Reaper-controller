using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

//[ExecuteInEditMode]
public class AudioObjectIEM : MonoBehaviour
{
    [Range(0, 11)]
    public int Color;

    public bool AlwaysSendPosition = true;

    private int _prevTrackNum;
    public int PortIn;
    public int PortOut;
    
    private Manipulate _manipulate;
    private bool _isGrabbed
    {
        get
        {
            if (!_manipulate) _manipulate = gameObject.GetComponent<Manipulate>();
            return _manipulate.IsGrabbing;
        }
    }

    private GameObject _nametag;

    private OscIn _oscInRoomEncoder;
    private OscOut _oscOutRoomEncoder;
    
    private OscMessage _roomX;
    private OscMessage _roomY;
    private OscMessage _roomZ;
    
    private Vector3 _pos;

    private OscMessage _sourceX;
    private OscMessage _sourceY;
    private OscMessage _sourceZ;
    
    private void Start()
    {
        var ip = GameObject.Find("OSC").GetComponent<OscManager>().RemoteIpAddress;

        if (PortOut > 0)
        {
            _oscOutRoomEncoder = gameObject.AddComponent<OscOut>();
            _oscOutRoomEncoder.Open(PortOut, ip);
            
            _sourceX = new OscMessage($"/RoomEncoder/sourceX");
            _sourceY = new OscMessage($"/RoomEncoder/sourceY");
            _sourceZ = new OscMessage($"/RoomEncoder/sourceZ");
        }

        if (PortIn > 0)
        {
            _oscInRoomEncoder = gameObject.AddComponent<OscIn>();
            _oscInRoomEncoder.Open(PortIn);
            
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/sourceX", OnReceiveSourceX );
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/sourceY", OnReceiveSourceY );
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/sourceZ", OnReceiveSourceZ );
        }

        SendPositionToRoomEncoder();
    }

    void OnReceiveSourceX(float val)
    {
        if (!_isGrabbed)
        {
            transform.position = new Vector3(transform.position.x,transform.position.y,val);
        }
    }
    
    void OnReceiveSourceY(float val)
    {
        if (!_isGrabbed)
        {
            transform.position = new Vector3(-val,transform.position.y,transform.position.z);
        }
    }
    
    void OnReceiveSourceZ(float val)
    {
        if (!_isGrabbed)
        {
            transform.position = new Vector3(transform.position.x,val,transform.position.z);
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
        SetColor(Color);
    }

    public void SetColor(int color)
    {
        Color = color;
        var meshRenderer = GetComponent<MeshRenderer>();
        if (meshRenderer.sharedMaterial.name != Color.ToString())
        {
            meshRenderer.sharedMaterial = Resources.Load<Material>($"Materials/AudioObject/{Color}");
        }
    }

    static float Scale(float inMin, float inMax, float outMin, float outMax, float val) =>
        Mathf.Lerp(
            outMin,
            outMax,
            Mathf.InverseLerp(inMin, inMax, val));

    private void SendPositionToRoomEncoder()
    {
        _pos = transform.position;
        _sourceX.Set(0, _pos.z);
        _sourceY.Set(0, -_pos.x);
        _sourceZ.Set(0, _pos.y);
        _oscOutRoomEncoder.Send(_sourceX);
        _oscOutRoomEncoder.Send(_sourceY);
        _oscOutRoomEncoder.Send(_sourceZ);
    }
    
    void Update()
    {
        UpdateNametag();

        if (_isGrabbed || AlwaysSendPosition)
        {
            SendPositionToRoomEncoder();
        }
    }
}
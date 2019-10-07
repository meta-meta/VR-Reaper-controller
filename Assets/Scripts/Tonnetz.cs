﻿using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Tonnetz : MonoBehaviour
{
    public GameObject prefab;
    public bool isColorChromatic;

    // Start is called before the first frame update
    void Start()
    {
        for (var s = 0; s < 10; s++)
        {
            for (var n = 0; n < 24; n++)
            {
                var note = 32 + n + s * 7;

                var notePad = GameObject.Instantiate(prefab, transform);
                notePad.transform.localPosition = new Vector3(n * 0.15f, 0, s * 0.15f);
                
                var mat = isColorChromatic ? note % 12 : (note * 7) % 12;
                notePad.GetComponent<MeshRenderer>().material = Resources.Load<Material>($"Materials/n{mat}");

                var notePadCmp = notePad.GetComponent<NotePad>();
                notePadCmp.oscAddr = "/tonnetz";
                notePadCmp.note = note;
            }
        }
    }

    // Update is called once per frame
    void Update()
    {
    }
}
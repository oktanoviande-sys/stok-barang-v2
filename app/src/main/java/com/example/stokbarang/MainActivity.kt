package com.example.stokbarang

import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.provider.Settings;import android.widget.*;import android.text.*;import org.json.*;import java.text.NumberFormat;import java.util.Locale

data class Item(var name:String,var cat:String,var color:String,var size:String,var modal:Long,var sell:Long,var stock:Int)
data class Tx(var name:String,var type:String,var qty:Int,var amount:Long,var time:Long)

class MainActivity:Activity(){
 private val items=mutableListOf<Item>(); private val tx=mutableListOf<Tx>(); lateinit var p:SharedPreferences
 lateinit var list:ListView; lateinit var search:EditText; lateinit var summary:TextView
 val money=NumberFormat.getCurrencyInstance(Locale("id","ID"))
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);p=getSharedPreferences("v2",0);load()
  list=findViewById(R.id.list);search=findViewById(R.id.search);summary=findViewById(R.id.summary)
  findViewById<Button>(R.id.add).setOnClickListener{edit(null)};findViewById<Button>(R.id.report).setOnClickListener{report()}
  search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,a:Int,c:Int,d:Int){};override fun afterTextChanged(e:Editable?){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){refresh()}})
  list.setOnItemClickListener{_,_,pos,_->menu(filtered()[pos])};refresh()
 }
 fun filtered()=items.filter{("${it.name} ${it.cat} ${it.color} ${it.size}").contains(search.text.toString(),true)}
 fun refresh(){val f=filtered();list.adapter=ArrayAdapter(this,android.R.layout.simple_list_item_2,f.map{"${it.name} | ${it.color} | ${it.size} | Stok ${it.stock}"} .toTypedArray())
  val low=items.count{it.stock<=2};summary.text="Jenis ${items.size} • Total ${items.sumOf{it.stock}} pcs • Modal ${money.format(items.sumOf{it.modal*it.stock})}\nStok menipis: $low"
 }
 fun edit(old:Item?){val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL
  fun e(h:String,v:String=""):EditText=EditText(this).apply{hint=h;setText(v)}
  val n=e("Nama barang",old?.name?:""),c=e("Kategori",old?.cat?:""),co=e("Warna",old?.color?:""),s=e("Ukuran",old?.size?:"")
  val m=e("Harga modal",old?.modal?.toString()?:"");m.inputType=2
  val j=e("Harga jual",old?.sell?.toString()?:"");j.inputType=2
  val q=e("Stok",old?.stock?.toString()?:"");q.inputType=2
  listOf(n,c,co,s,m,j,q).forEach{box.addView(it)}
  AlertDialog.Builder(this).setTitle(if(old==null)"Tambah Barang" else "Edit Barang").setView(box).setNegativeButton("Batal",null).setPositiveButton("Simpan"){_,_->
   val x=Item(n.text.toString(),c.text.toString(),co.text.toString(),s.text.toString(),m.text.toString().toLongOrNull()?:0,j.text.toString().toLongOrNull()?:0,q.text.toString().toIntOrNull()?:0)
   if(old==null)items.add(x) else {old.name=x.name;old.cat=x.cat;old.color=x.color;old.size=x.size;old.modal=x.modal;old.sell=x.sell;old.stock=x.stock};save();refresh()
  }.show()
 }
 fun menu(x:Item){AlertDialog.Builder(this).setTitle(x.name).setItems(arrayOf("Stok Masuk","Penjualan / Stok Keluar","Edit","Hapus")){_,w->when(w){0->stock(x,1);1->stock(x,-1);2->edit(x);3->del(x)}}.show()}
 fun stock(x:Item,d:Int){val e=EditText(this);e.hint="Jumlah";e.inputType=2
  AlertDialog.Builder(this).setTitle(if(d>0)"Stok Masuk" else "Penjualan").setView(e).setNegativeButton("Batal",null).setPositiveButton("Simpan"){_,_->
   val q=e.text.toString().toIntOrNull()?:0;if(q<=0)return@setPositiveButton
   if(d<0&&q>x.stock){Toast.makeText(this,"Stok tidak cukup",Toast.LENGTH_SHORT).show();return@setPositiveButton}
   x.stock+=d*q;tx.add(Tx(x.name,if(d>0)"MASUK" else "JUAL",q,if(d>0)x.modal*q:x.sell*q,System.currentTimeMillis()));save();refresh()
  }.show()
 }
 fun del(x:Item){AlertDialog.Builder(this).setTitle("Hapus barang?").setMessage(x.name).setNegativeButton("Batal",null).setPositiveButton("Hapus"){_,_->items.remove(x);save();refresh()}.show()}
 fun report(){val sold=tx.filter{it.type=="JUAL"};val omzet=sold.sumOf{it.amount};val profit=sold.sumOf{t->items.find{it.name==t.name}?.let{(it.sell-it.modal)*t.qty}?:0}
  AlertDialog.Builder(this).setTitle("Laporan Penjualan").setMessage("Transaksi terjual: ${sold.sumOf{it.qty}} pcs\nOmzet: ${money.format(omzet)}\nPerkiraan keuntungan: ${money.format(profit)}\n\nRiwayat: ${tx.size} transaksi").setPositiveButton("Tutup",null).setNeutralButton("Backup"){_,_->backup()}.show()
 }
 fun backup(){val data=JSONObject();val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("name",x.name);put("cat",x.cat);put("color",x.color);put("size",x.size);put("modal",x.modal);put("sell",x.sell);put("stock",x.stock)})};data.put("items",a);startActivity(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,data.toString(2))})}
 fun save(){val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("name",x.name);put("cat",x.cat);put("color",x.color);put("size",x.size);put("modal",x.modal);put("sell",x.sell);put("stock",x.stock)})};p.edit().putString("items",a.toString()).apply()}
 fun load(){try{val a=JSONArray(p.getString("items","[]"));for(i in 0 until a.length()){val o=a.getJSONObject(i);items.add(Item(o.getString("name"),o.optString("cat"),o.optString("color"),o.optString("size"),o.getLong("modal"),o.getLong("sell"),o.getInt("stock")))}}catch(_:Exception){}}
}

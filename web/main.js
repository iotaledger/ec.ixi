const $inputs = {};
let $tables = {};

// namespaces
const Gui = {};
const Gen = {};
const Api = {};
const Btn = {};
const Graph = {};

window.onload = function () {
    init_elements();
    init_functions();
    Gui.refresh_wallet();
    Gui.refresh_actors();
    Gui.refresh_cluster();
    Gui.refresh_transfers();
};

function trytes_to_hex(trytes) {
    const HEX = "0123456789ABCDEF";
    let hex = "";
    for(let i = 0; i < trytes.length; i++)
        hex += HEX[(trytes[i] === '9' ? 0 : (trytes.charCodeAt(i)-'A'.charCodeAt(0))+1)%16];
    return hex;
}

function identicon(trytes) {
    const identicon = new Identicon(trytes_to_hex(trytes), 16).toString();
    return $('<img width=16 height=16 class="identicon" src="data:image/png;base64,' + identicon + '">');
}

function init_elements() {

    $('input').each((_, $el) => { $inputs[$el.id] = $($el); });
    val("seed", random_trytes(81));
    val("merkle_tree_depth",5);
    val("cluster_trust", 1);
}

function get_table(id) {
    if(!$tables[id])
        $tables[id] = $('#'+id+" table");
    return $tables[id];
}

function init_functions() {

    const shorten = (hash) => $("<div>").addClass("copyable").text(hash.substr(0, 30) + "…")
        .append(identicon(hash))
        .click(() => {copy_to_clipboard(hash)})
        .attr("title", "click to copy ");

    const percentage = (number) => parseFloat(number * 100).toFixed(1) + "%";

    const prepare_transfer = (seed, index, address, balance) => {
        val("transfers_seed", seed);
        val("transfers_index", index);
        val("transfers_sender", address);
        val("transfers_receiver", "");
        val("transfers_remainder", "");
        val("transfers_value", balance);
        Gui.show("new_transfer")
    };

    const wallet_serialize = (entry, index) => [
        "#"+index,
        shorten(entry['address']),
        entry['balance'],
        $("<div>")
            .append(Gen.gen_cell_button("send", () => {prepare_transfer(val("seed"), index, entry['address'], entry['balance'])}))
    ];

    const actors_serialize = actor => [
        shorten(actor['address']),
        actor['merkle_tree_index']+"/"+actor['merkle_tree_capacity'],
        $("<div>")
            .append(Gen.gen_cell_button("issue", () => { val("issue_actor", actor['address']); Gui.show("issue"); }))
            .append(Gen.gen_cell_button("tick", () => { Api.issue_marker(actor['address'], "", "", () => { Gui.refresh_actors(); }); }))
            .append(Gen.gen_cell_button("✘", () => { Api.remove_actor(actor['address']) }).addClass("delete"))
    ];

    const cluster_serialize = entry => [
        shorten(entry['address']),
        entry['trust_abs'] + " ("+percentage(entry['trust_rel'])+")",
        $("<div>").append(Gen.gen_cell_button("markers", () => { Gui.refresh_markers(entry['address']); Gui.show("markers"); }))
            .append(Gen.gen_cell_button("✘", () => {Api.set_trust(entry['address'], 0)}).addClass("delete"))
    ];

    const transfers_serialize = hash => [
        shorten(hash),
        $("<div>").append(Gen.gen_cell_button("status", () => { Gui.load_transactions(hash) }))
            .append(Gen.gen_cell_button("✘", () => { Api.remove_transfer(hash) }).addClass("delete"))
    ];

    const transactions_serialize = entry => [
        shorten(entry['hash']),
        shorten(entry['address']),
        entry['value'],
        $("<div>").text(percentage(entry['confidence']))
            .append(Gen.gen_cell_button("visualize", () => { Api.get_tangle(entry['hash'], Gui.show_tangle) }))
            .append(Gen.gen_cell_button("details", () => { Gui.display_confidences(entry['confidences']); Gui.show("confidences"); }))
    ];

    const confidences_serialize = entry => [
        shorten(entry['actor']),
        percentage(entry['confidence'])
    ];

    const markers_serialize = entry => [
        shorten(entry['ref1']),
        shorten(entry['ref2']),
        percentage(entry['confidence'])
    ];

    Gui.display_balances = Gen.gen_display_function('wallet', ["", "address", "balance", ""], wallet_serialize);
    Gui.display_actors = Gen.gen_display_function("actors", ["address", "markers", ""], actors_serialize);
    Gui.display_cluster = Gen.gen_display_function("cluster", ["address", "trust", ""], cluster_serialize);
    Gui.display_transfers = Gen.gen_display_function("transfers", ["transfer", ""], transfers_serialize);
    Gui.display_transactions = Gen.gen_display_function("transactions", ["hash", "address", "value", "confidence"], transactions_serialize);
    Gui.display_confidences = Gen.gen_display_function("confidences", ["actor", "confidence"], confidences_serialize);
    Gui.display_markers = Gen.gen_display_function("markers", ["reference #1", "reference #2", "confidence"], markers_serialize);
}

/* ***** BUTTON ACTIONS ***** */

Btn.submit_transfer = () => {
    if(!Gui.validate_form('new_transfer'))
        return;
    const seed = val("transfers_seed");
    const index = parseInt(val("transfers_index"));
    const receiver = val("transfers_receiver");
    const remainder = val("transfers_remainder");
    const value = val("transfers_value");
    const check_balances = $inputs['transfers_check_balances'].is(":checked");
    const tips = [];
    const tip1 = val("transfer_tip1");
    const tip2 = val("transfer_tip2");
    if(tip1.length > 0) tips.push(tip1);
    if(tip2.length > 0) tips.push(tip2);
    Api.submit_transfer(seed, index, receiver, remainder, value, check_balances, tips,() => { Gui.refresh_transfers(); Gui.hide("new_transfer") });
};

Btn.issue_marker = () => {
    if(!Gui.validate_form('issue'))
        return;
    const actor = val("issue_actor");
    const trunk = val("issue_trunk");
    let branch = val("issue_branch");
    if(branch.length === 0)
        branch = trunk;
    Api.issue_marker(actor, trunk, branch, () => { Gui.hide("issue"); Gui.refresh_actors(); });
};

Btn.create_actor = () => {
    if(!Gui.validate_form('actors'))
        return;
    const merkle_tree_depth = parseInt(val("merkle_tree_depth"));
    const seed = random_trytes(81);
    const add_to_cluster = $inputs['add_to_cluster'].is(":checked");
    Api.create_actor(seed, merkle_tree_depth, 0,actor => { Gui.refresh_actors(); if(add_to_cluster) Api.set_trust(actor, 1); });
};

Btn.set_trust = () => {
    if(!Gui.validate_form('set_trust'))
        return;
    const address = val("cluster_address");
    const trust = parseFloat(val("cluster_trust"));
    Api.set_trust(address, trust);
};

Btn.change_balance = () => {
    if(!Gui.validate_form('change_balance'))
        return;
    const address = val("change_balance_address");
    const to_add = val("change_balance_to_add");
    Api.change_balance(address, to_add);
};

/* ***** HELPERS ***** */

function val(input, val = null) {
    return val === null ? $inputs[input].val() : $inputs[input].val(val);
}

function random_trytes(length) {
    let seed = "";
    for(let i = 0; i < length; i++)
        seed += random_tryte();
    return seed;
}

function random_tryte() {
    const TRYTES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ9";
    return TRYTES.charAt(Math.floor(Math.random()*27));
}

function copy_to_clipboard(message) {
    let input = document.createElement('input');
    input.value = message;
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
}

/* ***** GUI ***** */

Gui.refresh_markers = (actor) => Api.get_markers(actor, Gui.display_markers);
Gui.refresh_wallet = () => Api.get_balances(val("seed"), Gui.display_balances);
Gui.refresh_actors = () => Api.get_actors(Gui.display_actors);
Gui.refresh_cluster = () => Api.get_cluster(Gui.display_cluster);
Gui.refresh_transfers = () => Api.get_transfers(Gui.display_transfers);

Gui.load_transactions = (bundle_head) => {
    Api.get_transactions(bundle_head, (transactions) => { Gui.display_transactions(transactions); Gui.show("transactions"); });
};

Gui.handle_error = function (message) {
    Swal.fire({title: "Whoops!", html: message, type: "error"});
    console.log(message);
};

Gui.hide = (id) => { $('#'+id).addClass("hidden"); };
Gui.show = (id) => { $('#'+id).removeClass("hidden"); };

Gui.show_tangle = function (tangle) {
    Gui.show("tangle");
    $.each(tangle['nodes'], (index, node) => {
        node['id'] = node['id'].substr(0, 10)+"…";
        const value = parseInt(node['value']);
        node['group'] = isNaN(value) ? 0 : value < 0 ? 1 : value > 0 ? 3 : 2;
    });
    $.each(tangle['links'], (index, link) => {
        link['source'] = link['source'].substr(0, 10)+"…";
        link['target'] = link['target'].substr(0, 10)+"…";
        link['distance'] = 100;
    });
    Graph.load_graph(tangle);
}

Gui.validate_form = (id) => {
    const $form_inputs = $('#'+id+" input");
    for(let i = 0; i < $form_inputs.length; i++) {
        const $el = $($form_inputs[i]);
        const pattern = $el.attr("pattern");
        const regex = new RegExp(pattern);
        if(pattern && !$el.val().match(regex)) {
            $el.addClass("invalid");
            Gui.handle_error("field <code>" + $el.attr("placeholder") + "</code> does not match expected pattern: <code>" + pattern+"</code>");
            return false;
        }
        $el.removeClass("invalid");
    }

    return true;
};

/* ***** GENERATORS ***** */

Gen.gen_display_function = function (table_name, table_head, object_serializer) {
    let $table = get_table(table_name);
    return function(objects) {
        $table.html(objects.length > 0 ? Gen.gen_table_row(table_head, true) : $("<tr>").append($("<td>").text("(empty)")));
        $.each(objects, function (index, object) {
            $table.append(Gen.gen_table_row(object_serializer(object, index)));
        });
    };
};

/**
 * @param cells array of row's cell contents in correct order
 * @param th if enabled will use <th> cells instead of <td>
 * @return table row whose cells are filled with respective content
 * */
Gen.gen_table_row = function (cells, th = false) {
    const $tr = $("<tr>");
    $.each(cells, function (_, cell) {
        $tr.append($(th ? "<th>" : "<td>").html(cell));
    });
    return $tr;
};

Gen.gen_cell_button = (name, onclick) => $("<input>").attr("type", "button").val(name).click(onclick);

/* ***** API WRAPPERS ***** */

Api.submit_transfer = function (seed, index, receiver, remainder, value, check_balances, tips, callback) {
    const request ={
        "action": "submit_transfer",
        "seed": seed,
        "index": index,
        "receiver": receiver,
        "remainder": remainder,
        "value": value,
        "tips": tips,
        "check_balances": check_balances
    };
    Api.ec_request(request, response => callback(response['hash']));
};

Api.issue_marker = function (actor, trunk, branch, callback) {
    const request ={
        "action": "issue_marker",
        "actor": actor,
        "trunk": trunk,
        "branch": branch,
    };
    Api.ec_request(request, () => callback());
};

Api.get_transfers = function (callback) {
    Api.ec_request({"action": "get_transfers"}, response => callback(response['transfers']));
};

Api.get_transactions = function (bundle_head, callback) {
    Api.ec_request({"action": "get_transactions", "bundle_head": bundle_head}, response => callback(response['transactions']));
};

Api.get_balances = function (seed, callback) {
    Api.ec_request({"action": "get_balances", "seed": seed}, response => callback(response['balances']));
};

Api.get_actors = function (callback) {
    Api.ec_request({"action": "get_actors"}, response => callback(response['actors']));
};

Api.get_cluster = function (callback) {
    Api.ec_request({"action": "get_cluster"}, response => callback(response['cluster']));
};

Api.get_markers = function (actor, callback) {
    Api.ec_request({"action": "get_markers", "actor": actor}, response => callback(response['markers']));
};

Api.get_tangle = function (transaction, callback) {
    Api.ec_request({"action": "get_tangle", "transaction": transaction}, response => callback(response['tangle']));
};

Api.create_actor = function (seed, merkle_tree_depth, start_index, callback) {
    Api.ec_request({"action": "create_actor", "seed": seed, "depth": merkle_tree_depth, "index": start_index, "security_level": 3}, response => callback(response['address']));
};

Api.remove_actor = function (address) {
    Api.ec_request({"action": "delete_actor", "address": address}, Gui.refresh_actors);
};

Api.remove_transfer = function (transfer) {
    Api.ec_request({"action": "unwatch_transfer", "transfer": transfer}, Gui.refresh_transfers);
};

Api.set_trust = function (address, trust) {
    Api.ec_request({"action": "set_trust", "address": address, "trust": trust}, Gui.refresh_cluster);
};

Api.change_balance = function (address, to_add) {
    Api.ec_request({"action": "change_balance", "address": address, "to_add": to_add}, Gui.refresh_wallet);
};

/* ***** API ***** */

Api.ec_request = (request, success) => {
    Api.ajax("getModuleResponse", {"request": JSON.stringify(request), "path": "ec.ixi-1.0.jar"}, data => {
        const response = JSON.parse(data['response']);
        response['success'] ? (success ? success(response) : {}) : Gui.handle_error("api error: " + response['error'].replace(/[A-Z9]{81}/g, "<code class='hash'>$&</code>"));
    });
};

Api.ajax = (path, data, success) => {
    data['password'] = "change_me_now";
    $.ajax({
        url: "http://localhost:2187/" + path,
        method: "POST",
        data: Api.serialize_post_data(data),
        dataType: "json",
        success: success,
        error: (error) => {
            Gui.handle_error(error['status'] === 0 ? "Could not reach your Ict node. Is it running with API enabled on port 2187?" : JSON.stringify(error));
        }
    });
};

Api.serialize_post_data = (data) => {
    const serialized = [];
    $.each(data, function (name, value) {
        serialized.push({"name": name, "value": value});
    });
    return serialized;
};

// ***** D3 *****

    Graph.load_graph = function(graph) {

        $("#tangle svg").html("");

        const svg = d3.select("svg"),
            width = +svg.attr("width"),
            height = +svg.attr("height");

        const color = [
            "#DDDDDD",
            "#FF0044", // output
            "#BBBBBB", // zero value
            "#00BB88" // input
        ];

        const simulation = d3.forceSimulation()
            .force("link", d3.forceLink().distance(function(d) {return d.distance;}).id(function(d) { return d.id; }))
            .force("charge", d3.forceManyBody())
            .force("center", d3.forceCenter(width / 2, height / 2));

        const link = svg.append("g")
            .attr("class", "links")
            .selectAll("line")
            .data(graph.links)
            .enter().append("line")
            .attr("stroke-width", function(d) { return Math.sqrt(d.value); });

        const node = svg.append("g")
            .attr("class", "nodes")
            .selectAll("g")
            .data(graph.nodes)
            .enter().append("g");

        const circles = node.append("circle")
            .attr("r", 10)
            .attr("fill", function(d) { return color[d.group]; });

        const lables = node.append("text")
            .text(function(d) {
                return d.id;
            })
            .attr('x', 10)
            .attr('y', 10);

        node.append("title")
            .text(function(d) { return d.id + ""; });

        simulation
            .nodes(graph.nodes)
            .on("tick", ticked);

        simulation.force("link")
            .links(graph.links);

        function ticked() {
            link
                .attr("x1", function(d) { return d.source.x; })
                .attr("y1", function(d) { return d.source.y; })
                .attr("x2", function(d) { return d.target.x; })
                .attr("y2", function(d) { return d.target.y; });

            node
                .attr("transform", function(d) {
                    return "translate(" + d.x + "," + d.y + ")";
                })
        }
    };